package com.lulan.shincolle.inventory;

import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**CUSTOM SHIP INVENTORY
 * slot: S0(80,19) S1(80,37) S2(80,55) S3(80,73) S4(80,91) S5~S22(8,112)
 * player inventory(8,155) hotbar(8,213)
 * S0~S4 for equip only
 */
public class ContainerShipInventory extends Container {
	
	private BasicEntityShip entity;
	
	public ContainerShipInventory(InventoryPlayer invPlayer, BasicEntityShip entity1) {
		int i,j;	//loop index
		this.entity = entity1;
		
		//ship equip
		for(i=0; i<5; i++) {
			this.addSlotToContainer(new SlotShipInventory(entity1.ExtProps, i, 80, 19+i*18));
		}
		
		//ship inventory
		for(i=0; i<2; i++) {
			for(j=0; j<9; j++) {
				this.addSlotToContainer(new SlotShipInventory(entity1.ExtProps, j+i*9+5, 8+j*18, 112+i*18));
			}
		}
		
		//player inventory
		for(i=0; i<3; i++) {
			for(j=0; j<9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j+i*9+9, 8+j*18, 155+i*18));
			}
		}
		
		//player action bar (hot bar)
		for(i=0; i<9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8+i*18, 213));
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
	 * Click: slot 0~4   (Equip)   -> put in slot 5~58 (ShipInv & Player)
	 *        slot 5~22  (ShipInv) -> if equip -> slot 0~4 (Equip)
	 *                             -> if other -> slot 23~58 (Player)
	 *        slot 23~58 (Player)  -> if equip -> slot 0~4 (Equip)
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

            if(slotid < 5) {  		//click equip slot
            	if(!this.mergeItemStack(itemstack1, 5, 59, true)) { //take out equip
                	return null;
                }
                slot.onSlotChange(itemstack1, itemstack); //�Y���~���\�h�ʹL, �h�I�sslot change�ƥ�
            }
            else {					//slot is ship or player inventory (5~58)
            	if(slotid < 23) {	//if ship inventory (5~22)
            		if(isEquip) {	//��equip��islot 0~4, �뤣�U�h��player inventory (23~58)
            			if(!this.mergeItemStack(itemstack1, 0, 5, false)) {
                			if(!this.mergeItemStack(itemstack1, 23, 59, true)) {
                				return null;
                			}			
                        }
            		}  
            		else {			//non-equip, put into player inventory (23~58)
            			if(!this.mergeItemStack(itemstack1, 23, 59, false)) {
            				return null;
            			}
            		}
            	}
            	else {				//if player inventory (23~58)
            		if(isEquip) {	//��equip��islot 0~4, �뤣�U�h��ship inventory (5~22)
            			if(!this.mergeItemStack(itemstack1, 0, 5, false)) {
                			if(!this.mergeItemStack(itemstack1, 5, 23, true)) {
                				return null;
                			}			
                        }
            		} 
            		else {			//non-equip, put into ship inventory (5~22)
            			if(!this.mergeItemStack(itemstack1, 5, 23, false)) {
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
	
	//�Ncontainer�ƭȸ�entity�����ƭȤ��, �p�G���P�h�o�e��s��gui�e�{�s�ƭ�
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();


    }

}
