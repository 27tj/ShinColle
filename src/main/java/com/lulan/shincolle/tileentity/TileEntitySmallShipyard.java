package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.block.BlockSmallShipyard;
import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.MathHelper;

public class TileEntitySmallShipyard extends BasicTileEntity implements ISidedInventory {

	private ItemStack slots[] = new ItemStack[6];	//0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output

	/** Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
	 *  Total Build Time = FuelCost / buildSpeed
	 *  MaxBuildTime / MaxFuelCost = 8min / 460800  (48 fuel per tick)
	 *  MinBuildTime / MinFuelCost = 1min / 57600
	 * 	MaxMaterial / MaxFuelCost = 64*4 / 460800
	 *  MinMaterial / MinFuelCost = 16*4 / 57600 = BaseCost(57600) CostPerMaterial(2100)
	 */	
	public int consumedPower = 0;					//consumed power (increase)
	public int remainedPower = 0;					//remained power (decrease)
	public int goalPower = 0;						//goal power (sum by material amount)
	private static final int buildSpeed = 480;  		//power cost per tick	
	private static final int maxPower = 460800; 	//max power storage
	private static final int[] slots_all = new int[] {0, 1, 2, 3, 4, 5};  //dont care side

	
	public TileEntitySmallShipyard() {}

	//�]�w�Ŷ����, �U��l�γ~:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	//����slot i��, �ƶqj�Ӫ��~, �^�Ǭ�itemstack, ���k�䵥�ʧ@�s��slot�ɷ|�I�s����k
	//(�Dshift�ʧ@) shift�ʧ@�bcontainer����transferStackInSlot����@
	@Override
	public ItemStack decrStackSize(int i, int j) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            if (itemStack.stackSize <= j) {			  //�Y�ƶq<=j��
                setInventorySlotContents(i, null);	  //�h��slot�M��
            }
            else {									  //�Y�ƶq >j��
                itemStack = itemStack.splitStack(j);  //��itemstack�ƶq-j
                if (itemStack.stackSize == 0) {
                    setInventorySlotContents(i, null);//��������, slot�M��
                }
            }
        }
        return itemStack;
	}
	
	//�̷ӿ�X�J�f�]�w, �M�w�|�浥�˸m�p���X�J���~��S�wslot��
	@Override
	public int[] getAccessibleSlotsFromSide(int i) {
		return slots_all;
		//return i == 0 ? slots_bottom : (i == 1 ? slots_top : slots_side);
	}

	//����gui�ɬO�_���Xslot�������~, �H�K�����~�����X��, �Ω�X���x����� (������S���Ψ�)
	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            setInventorySlotContents(i, null);
        }
        return itemStack;
	}

	//�Nslot�]���ؼ�itemstack(�]�i�H�]��null) �Ω�decrStackSize����k
	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		slots[i] = itemstack;
		//�Y��W���~�W�L�Ӯ�l����ƶq, �h�u���i����ƶq
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}	
	}

	//�T�{�O�_���ۭq�W��(�ϥ�name tag���W�L����)
	@Override
	public boolean hasCustomInventoryName() {
		return this.customName != null && this.customName.length() > 0;
	}
	
	//GUI��ܪ��W��, ��custom name�h��, ���M�N�ιw�]�W��
	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container."+Reference.MOD_ID+":SmallShipyard";
	}

	//�C��i�񪺳̤j�ƶq�W��
	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	//�O�_�i�H�k���I�}���
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		//�ѩ�|���h��tile entity�ƥ�, �n���T�{�y�ЬۦP���ƥ��~��ϥ�
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		}
		else {	//�T�{player�n�b��tile entity 64�椺, �H�K�W�XŪ���d��
			return player.getDistanceSq((double)xCoord+0.5D, (double)yCoord+0.5D, (double)zCoord+0.5D) <= 64;
		}
	}

	//�����k�����container�å�
	public void openInventory() {}
	public void closeInventory() {}
	
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
    }
	
	//�N��Ƽg�inbt
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		
		NBTTagList list = new NBTTagList();

		for(int i=0; i<slots.length; i++) {		//�Nslots[]��Ƽg�inbt
			if (slots[i] != null) {
				NBTTagCompound item = new NBTTagCompound();
				item.setByte("Slot", (byte)i);	//�btag: Slot�U�x�s���i
				slots[i].writeToNBT(item);		//�btag: Slot�U�x�sslots[i]���
				list.appendTag(item);			//�W�[�U�@�����
			}
		}
		
		compound.setTag("Items", list);
		compound.setInteger("consumedPower", consumedPower);
		compound.setInteger("remainedPower", remainedPower);
		compound.setInteger("goalPower", goalPower);
	}
	
	//�P�w���~�O�_���J�Ӯ�l, �Ω�canExtractItem����k
	//��l�γ~:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
		if(itemstack != null) {
			Item item = itemstack.getItem();
		
			switch(slot) {
			case 0:		//grudge slot
				return item == ModItems.Grudge;
			case 1:		//abyssium slot
				return item == ModItems.Abyssium;
			case 2:		//ammo slot
				return item == ModItems.Ammo;
			case 3:		//polymetal slot
				return item == ModItems.Polymetal;
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

	//�ϥκ޽u/�|���J�ɩI�s, ���A�Ω��ʸm�J
	@Override
	public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
		return this.isItemValidForSlot(slot, itemstack);  //����side �ҥΦ��P�w
	}

	//�ϥκ޽u/�|���X�ɩI�s, ���A�Ω��ʸm�J
	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
		//�u��output slot��fuel slot����bucket�i�H��X
		return (slot == 5) || (itemstack.getItem() == Items.bucket);
	}
	
	//�سyship��k
	public void buildShip() {
		byte[] matAmount = new byte[4];
		//���o�|�˧��Ƽƶq
		matAmount = SmallRecipes.getMaterialAmount(slots);

		//�N��J���ƥ����Y��
		slots[0] = null;
		slots[1] = null;
		slots[2] = null;
		slots[3] = null;
		//��J���Ƽƶq, ���obuild output��slot 5
		slots[5] = SmallRecipes.getBuildResult(matAmount);	
	}
	
	//�P�w�O�_�سy��
	public boolean isBuilding() {
		return hasRemainedPower() && canBuild();
	}
	
	//�P�w�O�_���U��
	public boolean hasRemainedPower() {
		return remainedPower > buildSpeed;
	}
	
	//�P�w�O�_���سy�ؼ�
	public boolean canBuild() {
		return goalPower > 0;
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
		boolean isActive = isBuilding();
		boolean sendUpdate = false;	//�Ь��nblock update, ���n��smetadata�ɳ]��true
		
		//update goalPower, check goalPower if material in slots[3] (polymetal slot)
		if(slots[3] != null) {
			this.getGoalPower();
		}
		else {
			this.goalPower = 0;
		}
		
		//server side
		if(!worldObj.isRemote) {
			//fuel�ɥR
			if(TileEntityFurnace.isItemFuel(this.slots[4]) && this.remainedPower < (this.maxPower - TileEntityFurnace.getItemBurnTime(this.slots[4]))) {
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
				this.remainedPower -= buildSpeed;	//fuel bar --
				this.consumedPower += buildSpeed;	//build bar ++
				
				//power�F��, �سy����
				if (this.consumedPower >= this.goalPower) {
					this.consumedPower = 0;
					this.goalPower = 0;
					this.buildShip();	//�سy�X���~���output slot
					sendUpdate = true;
				}
			}
			
			
			if(!this.canBuild()) {	//�D�سy��, ���mbuild bar
				this.consumedPower = 0;
			}
			
			//�Y���A�����ܹL, �h�o�e��s  ex:����active �ӿU�ƥΥ��ɭP�L�kactive��
			if(isActive != this.isBuilding()) {
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

	//�p��build�i�ױ�, i���i�ױ�pixel����
	public int getBuildProgressScaled(int i) {
		//goalPower+1 for avoid devide by zero
		return (consumedPower * i) / (goalPower + 1);
	}

	//�p��fuel�s�q��
	public int getPowerRemainingScaled(int i) {
		return (remainedPower * i) / maxPower;
	}
	
	//�p��سy�ɶ� (���⦨�u��ɶ�)
	public String getBuildTimeString() {
		int timeSec = (goalPower - consumedPower) / buildSpeed / 20;	//get time (���: sec)		
		return CalcHelper.getTimeFormated(timeSec);		
	}

	
}
