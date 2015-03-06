package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.block.BlockSmallShipyard;
import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.FormatHelper;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.MathHelper;

/** Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 8min / 460800  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 1min / 57600
 * 	MaxMaterial / MaxFuelCost = 64*4 / 460800
 *  MinMaterial / MinFuelCost = 16*4 / 57600 = BaseCost(57600) CostPerMaterial(2100)
 */
public class TileEntitySmallShipyard extends BasicTileEntity {
		
	private int consumedPower = 0;	//�w��O����q
	private int remainedPower = 0;	//�Ѿl�U��
	private int goalPower = 0;		//�ݭn�F�����ؼЯ�q
	private int buildType = 0;		//type 0:none 1:ship 2:equip
	private boolean isActive;		//�O�_���b�سy��, ��������isBuilding�O�_���ܤƥ�
	private static final int BUILDSPEED = 48;  	//power cost per tick	
	private static final int MAXPOWER = 460800; 	//max power storage
	private static final int[] ALLSLOTS = new int[] {0, 1, 2, 3, 4, 5};  //dont care side

	
	public TileEntitySmallShipyard() {
		//0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
		this.slots = new ItemStack[6];
		this.isActive = false;
		this.syncTime = 0;
	}

	//�̷ӿ�X�J�f�]�w, �M�w�|�浥�˸m�p���X�J���~��S�wslot��
	@Override
	public int[] getAccessibleSlotsFromSide(int i) {
		return ALLSLOTS;
		//return i == 0 ? slots_bottom : (i == 1 ? slots_top : slots_side);
	}

	//GUI��ܪ��W��, ��custom name�h��, ���M�N�ιw�]�W��
	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container."+Reference.MOD_ID+":SmallShipyard";
	}

	//�O�_�i�H�k���I�}���
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		//�ѩ�|���h��tile entity�ƥ�, �n���T�{�y�ЬۦP���ƥ��~��ϥ�
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		}
		else {	//�T�{player�n�b��tile entity 64�椺, �H�K�W�XŪ���d�� or ���ͨ�L����bug
			return player.getDistanceSq((double)xCoord+0.5D, (double)yCoord+0.5D, (double)zCoord+0.5D) <= 64;
		}
	}

	//Ū��nbt���
	@Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);	//�qnbtŪ�������xyz�y��

        NBTTagList list = compound.getTagList("Items", 10);	//��nbt tag: Items (��������10:TagCompound)
        
        for(int i=0; i<list.tagCount(); i++) {			//�Ntag�C�X���Ҧ����~��X��
            NBTTagCompound item = list.getCompoundTagAt(i);
            byte sid = item.getByte("Slot");
            
            if (sid>=0 && sid<slots.length) {	//Ū��nbt���������~, �ͦ���Uslot�� 
            	slots[sid] = ItemStack.loadItemStackFromNBT(item);
            }
        }

        consumedPower = compound.getInteger("consumedPower");
        remainedPower = compound.getInteger("remainedPower");
        goalPower = compound.getInteger("goalPower");
        buildType = compound.getInteger("buildType");
    }
	
	//�N��Ƽg�inbt
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		
		NBTTagList list = new NBTTagList();
		compound.setTag("Items", list);
		for(int i=0; i<slots.length; i++) {		//�Nslots[]��Ƽg�inbt
			if (slots[i] != null) {
				NBTTagCompound item = new NBTTagCompound();
				item.setByte("Slot", (byte)i);	//�btag: Slot�U�x�s���i
				slots[i].writeToNBT(item);		//�btag: Slot�U�x�sslots[i]���
				list.appendTag(item);			//�W�[�U�@�����
			}
		}
			
		compound.setInteger("consumedPower", consumedPower);
		compound.setInteger("remainedPower", remainedPower);
		compound.setInteger("goalPower", goalPower);
		compound.setInteger("buildType", buildType);
	}
	
	//�P�w���~�O�_���J�Ӯ�l, �Ω�canExtractItem����k
	//��l�γ~:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
		if(itemstack != null) {
			Item item = itemstack.getItem();
			int meta = itemstack.getItemDamage();
		
			switch(slot) {
			case 0:		//grudge slot
				return item == ModItems.Grudge;
			case 1:		//abyssium slot
				return item == ModItems.AbyssMetal && meta == 0;
			case 2:		//ammo slot
				return item == ModItems.Ammo && meta == 0;
			case 3:		//polymetal slot
				return item == ModItems.AbyssMetal && meta == 1;
			case 4:		//fuel slot
				return TileEntityFurnace.isItemFuel(itemstack);
			default:
				return false;
			}
		}
		else {
			return false;
		}
	}

	//�ϥκ޽u/�|���X�ɩI�s, ���A�Ω��ʸm�J
	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
		//�u��output slot��fuel slot����bucket�i�H��X
		return (slot == 5) || (itemstack.getItem() == Items.bucket);
	}
	
	//�سyship��k
	public void buildComplete() {
		byte[] matAmount = new byte[4];
		//���o�|�˧��Ƽƶq
		matAmount = SmallRecipes.getMaterialAmount(slots);

		//�N��J���ƥ����Y��
		slots[0] = null;
		slots[1] = null;
		slots[2] = null;
		slots[3] = null;
		
		//��J���Ƽƶq, ���obuild output��slot 5
		if(this.buildType == 1) {	//build ship
			slots[5] = SmallRecipes.getBuildResultShip(matAmount);
		}
		else {						//build equip or no select
			slots[5] = SmallRecipes.getBuildResultEquip(matAmount);
		}
		
	}
	
	//�P�w�O�_�سy��
	public boolean isBuilding() {
		return hasRemainedPower() && canBuild();
	}
	
	//�P�w�O�_���U��
	public boolean hasRemainedPower() {
		return remainedPower > BUILDSPEED;
	}
	
	//�P�w�O�_���سy�ؼ�
	public boolean canBuild() {
		return goalPower > 0 && slots[5] == null;
	}
	
	//���o�سy��O
	public void getGoalPower() {
		byte[] itemAmount = new byte[4];	
		//�p����ƶq
		itemAmount = SmallRecipes.getMaterialAmount(slots);		
		//�̷ӧ��ƶq�p��goalPower, �Y���ƨS�FminAmount�hgoalPower�|�o��0
		goalPower = SmallRecipes.calcGoalPower(itemAmount); 
	}
	
	//������y�{�i���k
	//��ƥ����HmarkDirty�аOblock��s, �H��Ū�gNBT tag�ӫO�s
	@Override
	public void updateEntity() {
		boolean sendUpdate = false;	//�Ь��nblock update, ���n��smetadata�ɳ]��true
		
		//update goalPower, check goalPower if material in slots[3] (polymetal slot)
		if(this.buildType != 0) {
			this.getGoalPower();
		}
		else {
			this.goalPower = 0;
		}
		
		//server side
		if(!worldObj.isRemote) {
			//fuel�ɥR
			if(TileEntityFurnace.isItemFuel(this.slots[4]) && this.remainedPower < (this.MAXPOWER - TileEntityFurnace.getItemBurnTime(this.slots[4]))) {
				this.remainedPower += TileEntityFurnace.getItemBurnTime(this.slots[4]);
				
				if(this.slots[4] != null) {
					sendUpdate = true;			//�Ь��nupdate block
					this.slots[4].stackSize--;	//fuel -1
					
					if(this.slots[4].stackSize == 0) {
						this.slots[4] = this.slots[4].getItem().getContainerItem(this.slots[4]);
					}
				}
			}
			
			//�P�w�O�_�سy��, �Ctick�i��i�׭ȧ�s, �Y�D�سy���h���m�i�׭�
			if(this.isBuilding()) {
				this.remainedPower -= BUILDSPEED;	//fuel bar --
				this.consumedPower += BUILDSPEED;	//build bar ++
				
				//power�F��, �سy����
				if (this.consumedPower >= this.goalPower) {
					this.buildComplete();	//�سy�X���~���output slot
					this.consumedPower = 0;
					this.goalPower = 0;		
					this.buildType = 0;
					sendUpdate = true;
				}
			}
			
			if(!this.canBuild()) {	//�D�سy��, ���mbuild bar
				this.consumedPower = 0;
			}
			
			//�Y���A�����ܹL, �h�o�e��s  ex:����active �ӿU�ƥΥ��ɭP�L�kactive��
			if(isActive != this.isBuilding()) {
				isActive = this.isBuilding();
				sendUpdate = true;
			}
		}
		
		//�Ь��n��s
		if(sendUpdate) {
			//��s���metadata
			BlockSmallShipyard.updateBlockState(this.isBuilding(), this.worldObj, this.xCoord, this.yCoord, this.zCoord);
			//�аO������n��s, �H�O�Ҹ�Ʒ|�s��w��
			this.markDirty();
		}
	}

	//�p��fuel�s�q��
	public int getPowerRemainingScaled(int i) {
		return (remainedPower * i) / MAXPOWER;
	}
	
	//�p��سy�ɶ� (���⦨�u��ɶ�)
	public String getBuildTimeString() {
		//�Ѿl��� = (�ؼЯ�q - �ثe��q) / (�Ctick�W�[��q) / 20
		int timeSec = (goalPower - consumedPower) / BUILDSPEED / 20;	//get time (���: sec)		
		return FormatHelper.getTimeFormated(timeSec);
	}
	
	//getter
	public int getPowerConsumed() {
		return this.consumedPower;
	}
	public int getPowerRemained() {
		return this.remainedPower;
	}
	public int getPowerGoal() {
		return this.goalPower;
	}
	public int getBuildType() {
		return this.buildType;
	}
	
	//setter
	public void setPowerConsumed(int par1) {
		this.consumedPower = par1;
	}
	public void setPowerRemained(int par1) {
		this.remainedPower = par1;
	}
	public void setPowerGoal(int par1) {
		this.goalPower = par1;
	}
	public void setBuildType(int par1) {
		this.buildType = par1;
	}

	
}
