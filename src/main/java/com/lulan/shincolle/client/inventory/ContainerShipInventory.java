package com.lulan.shincolle.client.inventory;

import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**CUSTOM SHIP INVENTORY
 * slot: S0(66,18) S1(66,36) S2(66,54) S3(66,72) S4(66,90) S5(6,108) S6~S23(8,18) 6x3
 * player inventory(44,132) hotbar(44,190)
 * S0~S5 for equip only
 */
public class ContainerShipInventory extends Container {
	
	private BasicEntityShip entity;
	public static final byte SLOTS_TOTAL = 24;
	public static final byte SLOTS_EQUIP = 6;
	public static final byte SLOTS_INVENTORY = 18;
	private int GuiKills;
	private int GuiExpCurrent;
	private int GuiNumAmmo;
	private int GuiNumAmmoHeavy;
	private int GuiNumGrudge;
	private int ButtonAmmoLight;
	private int ButtonAmmoHeavy;
	
	public ContainerShipInventory(InventoryPlayer invPlayer, BasicEntityShip entity1) {
		int i,j;	//loop index
		this.entity = entity1;
		
		//ship equip = 0~5
		for(i=0; i<6; i++) {
			this.addSlotToContainer(new SlotShipInventory(entity1.getExtProps(), i, 66, 18+i*18));
		}
		
		//ship inventory = 6~23
		for(i=0; i<6; i++) {
			for(j=0; j<3; j++) {
				this.addSlotToContainer(new SlotShipInventory(entity1.getExtProps(), j+i*3+6, 8+j*18, 18+i*18));
			}
		}
		
		//player inventory
		for(i=0; i<3; i++) {
			for(j=0; j<9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j+i*9+9, 44+j*18, 132+i*18));
			}
		}
		
		//player action bar (hot bar)
		for(i=0; i<9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 44+i*18, 190));
		}
	}
	
	@Override
	public boolean canInteractWith(EntityPlayer p_75145_1_) {
		return true;
	}
	
	/**��container�䴩shift�I���~���ʧ@
	 * shift�I�H���I�]�������~->�P�w���~�����e����w��l, �Icontainer�������~->�e��H���I�]
	 * mergeItemStack: parm: item,start slot,end slot(���椣�P�w��J),�O�_��hotbar�}�l�P�w
	 * slot id: 0~4:equip  5~22:ship inventory 
	 *          23~49:player inventory  50~58:hot bar
	 *          
	 * Click: slot 0~5   (Equip)   -> put in slot 5~58 (ShipInv & Player)
	 *        slot 6~23  (ShipInv) -> if equip -> slot 0~4 (Equip)
	 *                             -> if other -> slot 23~58 (Player)
	 *        slot 24~59 (Player)  -> if equip -> slot 0~4 (Equip)
	 *        					   -> if other -> slot 5~22 (ShipInv)
	 *        
	 * Equip slot check in SlotShipInventory.class 
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
        ItemStack itemstack = null;
        Slot slot = (Slot)this.inventorySlots.get(slotid);
        boolean isEquip = false;

        if(slot != null && slot.getHasStack()) { 			//�Yslot���F��
            ItemStack itemstack1 = slot.getStack();			//itemstack1���o��slot���~
            itemstack = itemstack1.copy();					//itemstack�ƻs�@��itemstack1
            
            if(itemstack1.getItem() instanceof BasicEquip) isEquip = true;	//�P�w�O�_��equip

            if(slotid < 6) {  		//click equip slot
            	if(!this.mergeItemStack(itemstack1, 6, 60, true)) { //take out equip
                	return null;
                }	
                slot.onSlotChange(itemstack1, itemstack); //�Y���~���\�h�ʹL, �h�I�sslot change�ƥ�
            }
            else {					//slot is ship or player inventory (5~58)
            	if(slotid < 24) {	//if ship inventory (5~22)
            		if(isEquip) {	//��equip��islot 0~4, �뤣�U�h��player inventory (23~58)
            			if(!this.mergeItemStack(itemstack1, 0, 6, false)) {
                			if(!this.mergeItemStack(itemstack1, 24, 60, true)) {
                				return null;
                			}			
                        }
            		}  
            		else {			//non-equip, put into player inventory (23~58)
            			if(!this.mergeItemStack(itemstack1, 24, 60, true)) {
            				return null;
            			}
            		}
            	}
            	else {				//if player inventory (23~58)
            		if(isEquip) {	//��equip��islot 0~4, �뤣�U�h��ship inventory (5~22)
            			if(!this.mergeItemStack(itemstack1, 0, 6, false)) {
                			if(!this.mergeItemStack(itemstack1, 6, 24, true)) {
                				return null;
                			}			
                        }
            		} 
            		else {			//non-equip, put into ship inventory (5~22)
            			if(!this.mergeItemStack(itemstack1, 6, 24, false)) {
            				return null;
            			}
            		}
            	}
            }

            //�p�G���~���񧹤F, �h�]��null�M�ŸӪ��~
            if (itemstack1.stackSize == 0) {
                slot.putStack((ItemStack)null);
            }
            else { //�٨S��, ���]�@��slot update
                slot.onSlotChanged();
            }

            //�p�Gitemstack���ƶq�������ƶq�ۦP, ��ܳ����ಾ�ʪ��~
            if (itemstack1.stackSize == itemstack.stackSize) {
                return null;
            }
            //�̫�A�o�e�@��slot update
            slot.onPickupFromSlot(player, itemstack1);
        }
        return itemstack;	//���~���ʧ���, �^�ǳѤU�����~
    }
	
	//�o�e��sgui�i�ױ���s, ��detectAndSendChanges�٭n�u��(�b����minit��k��)
	@Override
	public void addCraftingToCrafters (ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		crafting.sendProgressBarUpdate(this, 0, this.entity.getKills());
		crafting.sendProgressBarUpdate(this, 1, this.entity.getExpCurrent());
		crafting.sendProgressBarUpdate(this, 2, this.entity.getNumAmmoLight());
		crafting.sendProgressBarUpdate(this, 3, this.entity.getNumAmmoHeavy());
		crafting.sendProgressBarUpdate(this, 4, this.entity.getNumGrudge());
		LogHelper.info("DEBUG : crafting update UseAL "+this.entity.getEntityFlagI(AttrID.F_UseAmmoLight));
		crafting.sendProgressBarUpdate(this, 5, this.entity.getEntityFlagI(AttrID.F_UseAmmoLight));
		crafting.sendProgressBarUpdate(this, 6, this.entity.getEntityFlagI(AttrID.F_UseAmmoHeavy));
	}
	
	//�����ƭȬO�_����, �����ܮɵo�e��s(����server�ݰ���)
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		int getValue;
		
        for(Object crafter : this.crafters) {
            ICrafting icrafting = (ICrafting) crafter;
            
            getValue = this.entity.getKills();
            if(this.GuiKills != getValue) {
                icrafting.sendProgressBarUpdate(this, 0, getValue);
                this.GuiKills = getValue;
            }   
            getValue = this.entity.getExpCurrent();
            if(this.GuiExpCurrent != getValue) {
                 icrafting.sendProgressBarUpdate(this, 1, getValue);
                 this.GuiExpCurrent = getValue;
            }
            getValue = this.entity.getNumAmmoLight();
            if(this.GuiNumAmmo != getValue) {
                icrafting.sendProgressBarUpdate(this, 2, getValue);
                this.GuiNumAmmo = getValue;
            }
            getValue = this.entity.getNumAmmoHeavy();
            if(this.GuiNumAmmoHeavy != getValue) {
                icrafting.sendProgressBarUpdate(this, 3, getValue);
                this.GuiNumAmmoHeavy = getValue;
            }
            getValue = this.entity.getNumGrudge();
            if(this.GuiNumGrudge != getValue) {
                icrafting.sendProgressBarUpdate(this, 4, getValue);
                this.GuiNumGrudge = getValue;
            }
            getValue = this.entity.getEntityFlagI(AttrID.F_UseAmmoLight);
            if(this.ButtonAmmoLight != getValue) {
                icrafting.sendProgressBarUpdate(this, 5, getValue);
                this.ButtonAmmoLight = getValue;
            }
            getValue = this.entity.getEntityFlagI(AttrID.F_UseAmmoHeavy);
            if(this.ButtonAmmoHeavy != getValue) {
                icrafting.sendProgressBarUpdate(this, 6, getValue);
                this.ButtonAmmoHeavy = getValue;
            }
        }
    }
	
	//client��container�����s��
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int valueType, int updatedValue) {     
		switch(valueType) {
		case 0: 
			LogHelper.info("DEBUG : client container set KILL"+updatedValue);
			this.entity.setKills(updatedValue);
			break;
		case 1:
			LogHelper.info("DEBUG : client container set EXP"+updatedValue);
			this.entity.setExpCurrent(updatedValue);
			break;
		case 2:
			LogHelper.info("DEBUG : client container set NAL"+updatedValue);
			this.entity.setNumAmmoLight(updatedValue);
			break;
		case 3:
			LogHelper.info("DEBUG : client container set NAH"+updatedValue);
			this.entity.setNumAmmoHeavy(updatedValue);
			break;
		case 4:
			LogHelper.info("DEBUG : client container set NG"+updatedValue);
			this.entity.setNumGrudge(updatedValue);
			break;
		case 5:
			LogHelper.info("DEBUG : client container set UseAL"+updatedValue);
			this.entity.setEntityFlagI(AttrID.F_UseAmmoLight, updatedValue);
			break;
		case 6:
			LogHelper.info("DEBUG : client container set UseAH"+updatedValue);
			this.entity.setEntityFlagI(AttrID.F_UseAmmoHeavy, updatedValue);
			break;
		}
    }

}
