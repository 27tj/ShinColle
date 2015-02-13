package com.lulan.shincolle.tileentity;

import java.util.List;

import com.lulan.shincolle.block.BlockGrudgeHeavy;
import com.lulan.shincolle.block.BlockSmallShipyard;
import com.lulan.shincolle.crafting.LargeRecipes;
import com.lulan.shincolle.entity.renderentity.BasicRenderEntity;
import com.lulan.shincolle.entity.renderentity.EntityRenderVortex;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.FormatHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;

/** Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 24min / 1382400  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 8min / 460800
 * 	MaxMaterial / MaxFuelCost = 1000*4 / 1382400
 *  MinMaterial / MinFuelCost = 100*4 / 460800 = BaseCost(460800) CostPerMaterial(256)
 */
public class TileMultiGrudgeHeavy extends BasicTileMulti {	
	
	private int powerConsumed = 0;	//�w��O����q
	private int powerRemained = 0;	//�Ѿl�U��
	private int powerGoal = 0;		//�ݭn�F�����ؼЯ�q
	private int buildType = 0;		//type 0:none 1:ship 2:equip
	private int invMode = 0;		//���~��Ҧ� 0:�����~ 1:��X���~
	private int selectMat = 0;		//���~��ܼҦ�, �Ω󪫫~��X 0:grudge 1:abyss 2:ammo 3:poly
	private boolean isActive;		//�O�_���b�سy��, ��������isBuilding�O�_���ܤƥ�
	private int[] matsBuild;		//�سy���ƶq
	private int[] matsStock;		//�w�s���ƶq
	public static final int BUILDSPEED = 48;  	//power cost per tick
	public static final int POWERMAX = 1382400; 	//max power storage
	public static final int SLOTS_NUM = 10;
	public static final int SLOTS_OUT = 0;
	private static final int[] SLOTS_ALL = new int[] {0,1,2,3,4,5,6,7,8,9};

	
	public TileMultiGrudgeHeavy() {
		//0:output 2~10:inventory
		this.slots = new ItemStack[SLOTS_NUM];
		this.isActive = false;
		this.matsBuild = new int[] {0,0,0,0};
		this.matsStock = new int[] {0,0,0,0};
	}
	
	//�̷ӿ�X�J�f�]�w, �M�w�|�浥�˸m�p���X�J���~��S�wslot��
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		//slot 0:output 2~10:inventory
		//side 0:bottom 1:top 2~5:side
		return SLOTS_ALL;
	}
	
	//GUI��ܪ��W��, ��custom name�h��, ���M�N�ιw�]�W��
	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container."+Reference.MOD_ID+":LargeShipyard";
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

        powerConsumed = compound.getInteger("powerConsumed");
        powerRemained = compound.getInteger("powerRemained");
        powerGoal = compound.getInteger("powerGoal");
        buildType = compound.getInteger("buildType");
        invMode = compound.getInteger("invMode");
        selectMat = compound.getInteger("selectMat");
        matsBuild = compound.getIntArray("matsBuild");
        matsStock = compound.getIntArray("matsStock");
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
			
		compound.setInteger("powerConsumed", powerConsumed);
		compound.setInteger("powerRemained", powerRemained);
		compound.setInteger("powerGoal", powerGoal);
		compound.setInteger("buildType", buildType);
		compound.setInteger("invMode", invMode);
		compound.setInteger("selectMat", selectMat);
		compound.setIntArray("matsBuild", matsBuild);
		compound.setIntArray("matsStock", matsStock);
	}
	
	//�P�w���~�O�_���J�Ӯ�l, �Ω�canExtractItem����k
	//��l�γ~:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
		if(slot == 0) {	//output slot
			return false;
		}	
		return true;
	}
	
	//�ϥκ޽u/�|���X�ɩI�s, ���A�Ω��ʸm�J
	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
		return true;
	}
	
	//�سyship��k
	public void buildComplete() {
		//��J���Ƽƶq, ���obuild output��slot 5
		if(this.buildType == 1) {	//build ship
			slots[0] = LargeRecipes.getBuildResultShip(matsBuild);
		}
		else {						//build equip or no select
			slots[0] = LargeRecipes.getBuildResultEquip(matsBuild);
		}
				
		//�N�سy���Ʀ���
		matsBuild[0] = 0;
		matsBuild[1] = 0;
		matsBuild[2] = 0;
		matsBuild[3] = 0;	
	}
	
	//�P�w�O�_�سy��
	public boolean isBuilding() {
		return hasPowerRemained() && canBuild();
	}
	
	//�P�w�O�_���U��
	public boolean hasPowerRemained() {
		return powerRemained > BUILDSPEED;
	}
	
	//�P�w�O�_���سy�ؼ�
	public boolean canBuild() {
		return powerGoal > 0 && slots[0] == null;
	}
	
	//������y�{�i���k
	//��ƥ����HmarkDirty�аOblock��s, �H��Ū�gNBT tag�ӫO�s
	@Override
	public void updateEntity() {
		boolean sendUpdate = false;	//�Ь��nblock update, ���n��smetadata�ɳ]��true

		//update goalPower, check goalPower if material in slots[3] (polymetal slot)
		if(this.buildType != 0) {
			this.powerGoal = LargeRecipes.calcGoalPower(matsBuild);
		}
		else {
			this.powerGoal = 0;
		}
		
		//server side
		if(!worldObj.isRemote) {
			//fuel�ɥR
			//1.��X���~�椤�̾a���䪺�U�� 2.����O�_�i�H�W�[�ӿU�� 3.�W�[�U��
			int burnTime;
			for(int i = SLOTS_OUT + 1; i < SLOTS_NUM; i++) {
				burnTime = TileEntityFurnace.getItemBurnTime(this.slots[i]);
				if(burnTime > 0 && burnTime + this.powerRemained < this.POWERMAX) {
					this.slots[i].stackSize--;	//fuel -1
					this.powerRemained += burnTime;
					
					//�Y�Ӫ��~�Χ�, ��getContainerItem�B�z�O�_�n�M���٬O�d�U��l ex: lava bucket -> empty bucket
					if(this.slots[i].stackSize == 0) {
						this.slots[i] = this.slots[i].getItem().getContainerItem(this.slots[i]);
					}
					
					sendUpdate = true;	//�Ь��nupdate block
					break;	//�[�L�@�ӿU�ƴN����loop, �Ctick�̦h�Y���@���U��
				}
			}
			
			//inventory mode 0:���J���~ 1:��X���~
			int itemType;
			if(invMode == 0) {	//���J���~
				for(int i = SLOTS_OUT + 1; i < SLOTS_NUM; i++) {
					itemType = LargeRecipes.getMaterialType(slots[i]);
					if(itemType > 0) {	//is material
						if(LargeRecipes.addMaterialStock(this, i, itemType)) {
							slots[i].stackSize--;
							
							if(slots[i].stackSize == 0) {
								slots[i] = null;
							}
							
							sendUpdate = true;
							break;		//�s�W���Ʀ��\, ����U��tick
						}
						//�s�W���ƥ���, �j�M�U�@��slot
					}
				}
			}
			else {				//��X���~
				if(getMatStock(selectMat) > 8) {		//��Xblock or container�����Y���A
					if(LargeRecipes.outputMaterialToSlot(this, selectMat, 1)) {
						this.addMatStock(selectMat, -9);
						sendUpdate = true;
					}
				}
				else if(getMatStock(selectMat) > 0) {	//��X��󪫫~���A
					if(LargeRecipes.outputMaterialToSlot(this, selectMat, 0)) {
						this.addMatStock(selectMat, -1);
						sendUpdate = true;
					}
				}
			}

			//�P�w�O�_�سy��, �Ctick�i��i�׭ȧ�s, �Y�D�سy���h���m�i�׭�
			if(this.isBuilding()) {
				this.powerRemained -= BUILDSPEED;	//fuel bar --
				this.powerConsumed += BUILDSPEED;	//build bar ++
				
				//power�F��, �سy����
				if (this.powerConsumed >= this.powerGoal) {
					this.buildComplete();	//�سy�X���~���output slot
					this.powerConsumed = 0;
					this.powerGoal = 0;
					this.buildType = 0;	
					sendUpdate = true;
				}
			}			
			
			if(!this.canBuild()) {	//�D�سy��, ���mbuild bar
				this.powerConsumed = 0;
			}
			
			//�Y���A�����ܹL, �h�o�e��s  ex:����active �ӿU�ƥΥ��ɭP�L�kactive��
			if(this.isActive != this.isBuilding()) {
				this.isActive = this.isBuilding();
				
				//set render entity state
				AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xCoord-1.5D, yCoord-2D, zCoord-1.5D, xCoord+1.5D, yCoord+1D, zCoord+1.5D);
				List renderEntityList = this.worldObj.getEntitiesWithinAABB(EntityRenderVortex.class, aabb);
				
	            for(int i = 0; i < renderEntityList.size(); i++) { 
	            	LogHelper.info("DEBUG : set render entity state "+this.isBuilding()+" "+renderEntityList.get(i)+xCoord+" "+yCoord+" "+zCoord);
	            	((EntityRenderVortex)renderEntityList.get(i)).setIsActive(this.isBuilding());
	            }
	            
				sendUpdate = true;
			}
		}
		else {	//client��, �ק�render entity���A
			//�Y���A�����ܹL, �h�o�e��s  ex:����active �ӿU�ƥΥ��ɭP�L�kactive��
			if(this.isActive != this.isBuilding()) {
				this.isActive = this.isBuilding();
				
				//set render entity state
				AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xCoord-1.5D, yCoord-2D, zCoord-1.5D, xCoord+1.5D, yCoord+1D, zCoord+1.5D);
				List renderEntityList = this.worldObj.getEntitiesWithinAABB(EntityRenderVortex.class, aabb);
				
	            for(int i = 0; i < renderEntityList.size(); i++) { 
	            	LogHelper.info("DEBUG : set render entity state "+this.isBuilding()+" "+renderEntityList.get(i)+xCoord+" "+yCoord+" "+zCoord);
	            	((EntityRenderVortex)renderEntityList.get(i)).setIsActive(this.isBuilding());
	            }
			}
		}
		
		//�Ь��n��s
		if(sendUpdate) {
//			int meta;
//			if(this.isBuilding()) { 
//				meta = 2;
//			}
//			else {
//				meta = 1;
//			}
//			
//			//��s���metadata
//			BlockGrudgeHeavy.updateBlockState(this.worldObj, this.xCoord, this.yCoord, this.zCoord, meta);
			//�аO������n��s, �H�O�Ҹ�Ʒ|�s��w��
			this.markDirty();
		}
	}

	//�p��fuel�s�q��
	public int getPowerRemainingScaled(int i) {
		return (powerRemained * i) / POWERMAX;
	}
	
	//�p��سy�ɶ� (���⦨�u��ɶ�)
	public String getBuildTimeString() {
		//�Ѿl��� = (�ؼЯ�q - �ثe��q) / (�Ctick�W�[��q) / 20
		int timeSec = (powerGoal - powerConsumed) / BUILDSPEED / 20;	//get time (���: sec)		
		return FormatHelper.getTimeFormated(timeSec);
	}
	
	//getter
	public int getPowerConsumed() {
		return powerConsumed;
	}
	public int getPowerRemained() {
		return powerRemained;
	}
	public int getPowerGoal() {
		return powerGoal;
	}
	public int getBuildType() {
		return buildType;
	}
	public int getInvMode() {
		return invMode;
	}
	public int getSelectMat() {
		return selectMat;
	}
	public int getMatBuild(int id) {
		return matsBuild[id];
	}
	public int getMatStock(int id) {
		return matsStock[id];
	}
	
	//setter
	public void setPowerConsumed(int par1) {
		this.powerConsumed = par1;
	}
	public void setPowerRemained(int par1) {
		this.powerRemained = par1;
	}
	public void setPowerGoal(int par1) {
		this.powerGoal = par1;
	}
	public void setBuildType(int par1) {
		this.buildType = par1;
	}
	public void setInvMode(int par1) {
		this.invMode = par1;
	}
	public void setSelectMat(int par1) {
		this.selectMat = par1;
	}
	public void setMatBuild(int id, int par1) {
		this.matsBuild[id] = par1;
	}
	public void setMatStock(int id, int par1) {
		this.matsStock[id] = par1;
	}
	public void addMatBuild(int id, int par1)  {	//add a number to build
		this.matsBuild[id] += par1;
	}
	public void addMatStock(int id, int par1)  {	//add a number to stock
		this.matsStock[id] += par1;
	}

}
