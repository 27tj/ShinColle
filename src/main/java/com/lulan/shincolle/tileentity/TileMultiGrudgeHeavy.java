package com.lulan.shincolle.tileentity;

import java.util.ArrayList;
import java.util.List;

import com.lulan.shincolle.block.BlockGrudgeHeavy;
import com.lulan.shincolle.block.BlockSmallShipyard;
import com.lulan.shincolle.crafting.LargeRecipes;
import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.entity.renderentity.BasicRenderEntity;
import com.lulan.shincolle.entity.renderentity.EntityRenderVortex;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.FormatHelper;
import com.lulan.shincolle.utility.LogHelper;
import com.lulan.shincolle.utility.TileEntityHelper;

import cpw.mods.fml.common.IFuelHandler;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/** Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 24min / 1382400  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 8min / 460800
 * 	MaxMaterial / MaxFuelCost = 1000*4 / 1382400
 *  MinMaterial / MinFuelCost = 100*4 / 460800 = BaseCost(460800) CostPerMaterial(256)
 */
public class TileMultiGrudgeHeavy extends BasicTileMulti implements ITileFurnace {	
	
	private int powerConsumed = 0;	//�w��O����q
	private int powerRemained = 0;	//�Ѿl�U��
	private int powerGoal = 0;		//�ݭn�F�����ؼЯ�q
	private int buildType = 0;		//type 0:none 1:ship 2:equip 3:ship loop 4: equip loop
	private int invMode = 0;		//���~��Ҧ� 0:�����~ 1:��X���~
	private int selectMat = 0;		//���~��ܼҦ�, �Ω󪫫~��X 0:grudge 1:abyss 2:ammo 3:poly
	private boolean isActive;		//�O�_���b�سy��, ��������isBuilding�O�_���ܤƥ�
	private int[] matsBuild;		//�سy���ƶq
	private int[] matsStock;		//�w�s���ƶq
	public static int buildSpeed = 48;  	//power cost per tick
	public static final int POWERMAX = 1382400; //max power storage
	public static final int SLOTS_NUM = 10;
	public static final int SLOTS_OUT = 0;
	public static final int[] SLOTS_ALL = new int[] {0,1,2,3,4,5,6,7,8,9};

	
	public TileMultiGrudgeHeavy() {
		//0:output 2~10:inventory
		this.slots = new ItemStack[SLOTS_NUM];
		this.isActive = false;
		this.matsBuild = new int[] {0,0,0,0};
		this.matsStock = new int[] {0,0,0,0};
		this.syncTime = 0;
		
		if(ConfigHandler.easyMode) {
			buildSpeed = 480;
		}
	}
	
	//�̷ӿ�X�J�f�]�w, �M�w�|�浥�˸m�p���X�J���~��S�wslot��
	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		//slot 0:output 2~10:inventory
		//side 0:bottom 1:top 2~5:side
		if(this.structType == 1 || this.structType == 2) {
			return SLOTS_ALL;
		}
		return new int[] {};
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
		switch(this.buildType) {
		default:
		case ID.Build.SHIP:			//build ship
		case ID.Build.SHIP_LOOP:
			slots[0] = LargeRecipes.getBuildResultShip(matsBuild);
			break;
		case ID.Build.EQUIP:		//build equip
		case ID.Build.EQUIP_LOOP:
			slots[0] = LargeRecipes.getBuildResultEquip(matsBuild);
			break;
		}
	}
	
	//�P�w�O�_�سy��
	public boolean isBuilding() {
		return hasPowerRemained() && canBuild();
	}
	
	//�P�w�O�_���U��
	public boolean hasPowerRemained() {
		return powerRemained > buildSpeed;
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
		
		//update goalPower
		if(this.buildType != 0) {
			this.powerGoal = LargeRecipes.calcGoalPower(matsBuild);
		}
		else {
			this.powerGoal = 0;
		}
		
		//server side
		if(!worldObj.isRemote) {
			this.syncTime++;
			
			//fuel�ɥR
			if(TileEntityHelper.checkItemFuel(this)) {
				sendUpdate = true;
			}
			
			//inventory mode 0:���J���~ 1:��X���~
			int itemType;
			if(invMode == 0) {	//���J���~
				for(int i = SLOTS_OUT + 1; i < SLOTS_NUM; i++) {
					itemType = LargeRecipes.getMaterialType(slots[i]);
					
					//add material into stock
					if(itemType > 0) {	//is material
						if(LargeRecipes.addMaterialStock(this, i, itemType)) {
							slots[i].stackSize--;
							
							if(slots[i].stackSize == 0) {
								slots[i] = null;
							}
							
							sendUpdate = true;
							break;		//�s�W���Ʀ��\, ����U��tick
						}
					}
				}
			}
			else {				//��X���~
				int compressNum = 9;	//output block
				int normalNum = 1;		//output single item
				
				//��X���~���ƶq
				if(ConfigHandler.easyMode) {	
					compressNum = 90;
					normalNum = 10;
				}
				
				//��Xblock or container�����Y���A
				if(getMatStock(selectMat) >= compressNum) {
					if(LargeRecipes.outputMaterialToSlot(this, selectMat, true)) {
						this.addMatStock(selectMat, -compressNum);
						sendUpdate = true;
					}
				}
				else if(getMatStock(selectMat) >= normalNum) {	//��X��󪫫~���A
					if(LargeRecipes.outputMaterialToSlot(this, selectMat, false)) {
						this.addMatStock(selectMat, -normalNum);
						sendUpdate = true;
					}
				}
			}

			//�P�w�O�_�سy��, �Ctick�i��i�׭ȧ�s, �Y�D�سy���h���m�i�׭�
			if(this.isBuilding()) {
				this.powerRemained -= buildSpeed;	//fuel bar --
				this.powerConsumed += buildSpeed;	//build bar ++
				
				//���Ӱ��t�سy����
				for(int i = SLOTS_OUT + 1; i < SLOTS_NUM; i++) {
					if(slots[i] != null && slots[i].getItem() == ModItems.InstantConMat) {
						slots[i].stackSize--;
						this.powerConsumed += 57600;
						
						if(this.slots[i].stackSize == 0) {
							this.slots[i] = null;
						}
						
						sendUpdate = true;
						break;
					}
				}
				
				//sync render entity every 40 ticks
				//set render entity state
				if(this.syncTime % 40 == 0) {
					this.sendSyncPacket();
					sendUpdate = true;
					this.syncTime = 0;
				}
				
				//power�F��, �سy����
				if (this.powerConsumed >= this.powerGoal) {
					this.buildComplete();	//�سy�X���~���output slot
					this.powerConsumed = 0;
					this.powerGoal = 0;

					//continue build if mode = loop mode
					switch(buildType) {
					default:
					case ID.Build.SHIP:
					case ID.Build.EQUIP:		//reset build type
						this.buildType = ID.Build.NONE;
						//�N�سy���ƲM��
						matsBuild[0] = 0;
						matsBuild[1] = 0;
						matsBuild[2] = 0;
						matsBuild[3] = 0;
						break;
					case ID.Build.SHIP_LOOP:	//remain build type
					case ID.Build.EQUIP_LOOP:	//remain build type
						this.setRepeatBuild();
						break;
					}

					sendUpdate = true;
				}
			}			
			
			if(!this.canBuild()) {	//�D�سy��, ���mbuild bar
				this.powerConsumed = 0;
			}
			
			//�Y���A�����ܹL, �h�o�e��s  ex:����active �ӿU�ƥΥ��ɭP�L�kactive��
			if(this.isActive != this.isBuilding()) {
				this.isActive = this.isBuilding();
				sendUpdate = true;
			}
		}
		
		//�Ь��n��s
		if(sendUpdate) {
			//set render entity state
			AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(xCoord-1.5D, yCoord-2D, zCoord-1.5D, xCoord+1.5D, yCoord+1D, zCoord+1.5D);
			List renderEntityList = this.worldObj.getEntitiesWithinAABB(EntityRenderVortex.class, aabb);
			
            for(int i = 0; i < renderEntityList.size(); i++) { 
//            	LogHelper.info("DEBUG : set render entity state (Tile class) "+this.isBuilding()+" "+renderEntityList.get(i)+xCoord+" "+yCoord+" "+zCoord);
            	((EntityRenderVortex)renderEntityList.get(i)).setIsActive(this.isBuilding());
            }

			this.markDirty();
		}
	}
	
	//set materials for repeat build
	public void setRepeatBuild() {
		//set materials
		for(int i = 0; i < 4; i++) {
			//has enough materials
			if(matsStock[i] >= matsBuild[i]) {
				matsStock[i] -= matsBuild[i];
			}
			//no materials, reset matsBuild
			else {
				matsBuild[i] = 0;
				buildType = ID.Build.NONE;
			}
		}
	}

	//�p��fuel�s�q��
	public int getPowerRemainingScaled(int i) {
		return (powerRemained * i) / POWERMAX;
	}
	
	//�p��سy�ɶ� (���⦨�u��ɶ�)
	public String getBuildTimeString() {
		//�Ѿl��� = (�ؼЯ�q - �ثe��q) / (�Ctick�W�[��q) / 20
		int timeSec = (powerGoal - powerConsumed) / buildSpeed / 20;	//get time (���: sec)		
		return FormatHelper.getTimeFormated(timeSec);
	}
	
	//getter
	@Override
	public int getPowerConsumed() {
		return powerConsumed;
	}
	@Override
	public int getPowerRemained() {
		return powerRemained;
	}
	@Override
	public int getPowerGoal() {
		return powerGoal;
	}
	@Override
	public int getPowerMax() {
		return POWERMAX;
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
	@Override
	public void setPowerConsumed(int par1) {
		this.powerConsumed = par1;
	}
	@Override
	public void setPowerRemained(int par1) {
		this.powerRemained = par1;
	}
	@Override
	public void setPowerGoal(int par1) {
		this.powerGoal = par1;
	}
	@Override
	public void setPowerMax(int par1) {}
	
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

	//fuel input slot (1~9)
	@Override
	public int getFuelSlotMin() {
		return SLOTS_OUT+1;
	}

	//fuel input slot (1~9)
	@Override
	public int getFuelSlotMax() {
		return SLOTS_NUM-1;
	}

	
}
