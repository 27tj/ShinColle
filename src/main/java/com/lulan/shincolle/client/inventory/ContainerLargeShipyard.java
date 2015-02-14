package com.lulan.shincolle.client.inventory;

import com.lulan.shincolle.crafting.LargeRecipes;
import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.tileentity.TileMultiGrudgeHeavy;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;

/**SLOT POSITION
 * output(168,51) fuel bar(9,83 height=63) fuel color bar(208,64)
 * ship button(157,24) equip button(177,24) inv(25,116)
 * player inv(25,141) action bar(25,199)
 */
public class ContainerLargeShipyard extends Container {
	
	private TileMultiGrudgeHeavy tile;
	private int guiPowerConsumed, guiPowerRemained, guiPowerGoal, guiBuildType, guiSelectMat, guiInvMode = 0;
	private int[] guiMatBuild = new int[4];
	private int[] guiMatStock = new int[4];
	private String guiBuildTime;
	
	//for shift item
	private static final int SLOT_OUTPUT = 0;
	private static final int SLOT_INVENTORY = 1;
	private static final int SLOT_PLAYERINV = 10;
	private static final int SLOT_HOTBAR = 37;
	private static final int SLOT_ALL = 46;
	
	
	public ContainerLargeShipyard(InventoryPlayer player, TileMultiGrudgeHeavy tile) {
		this.tile = tile;
		guiMatBuild[0] = tile.getMatBuild(0);
		guiMatBuild[1] = tile.getMatBuild(1);
		guiMatBuild[2] = tile.getMatBuild(2);
		guiMatBuild[3] = tile.getMatBuild(3);
		guiMatStock[0] = tile.getMatStock(0);
		guiMatStock[1] = tile.getMatStock(1);
		guiMatStock[2] = tile.getMatStock(2);
		guiMatStock[3] = tile.getMatStock(3);
			
		//output slot (0)
		this.addSlotToContainer(new SlotLargeShipyard(tile, 0, 168, 51));  //output
		
		int i,j;
		//inventory slot (1~9)
		for(i = 1; i < 10; i++) {
			this.addSlotToContainer(new SlotLargeShipyard(tile, i, 7+i*18, 116));
		}
	
		//player inventory
		for(i=0; i<3; i++) {
			for(j=0; j<9; j++) {
				this.addSlotToContainer(new Slot(player, j+i*9+9, 25+j*18, 141+i*18));
			}
		}
		
		//player action bar (hot bar)
		for(i=0; i<9; i++) {
			this.addSlotToContainer(new Slot(player, i, 24+i*18, 199));
		}
	}
	
	//�o�e��sgui�i�ױ���s, ��detectAndSendChanges�٭n�u��(�b����minit��k��)
	@Override
	public void addCraftingToCrafters (ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		crafting.sendProgressBarUpdate(this, 0, this.tile.getPowerConsumed());
		crafting.sendProgressBarUpdate(this, 1, this.tile.getPowerGoal());
		crafting.sendProgressBarUpdate(this, 2, this.tile.getPowerRemained());
		crafting.sendProgressBarUpdate(this, 3, this.tile.getBuildType());
		crafting.sendProgressBarUpdate(this, 4, this.tile.getSelectMat());
		crafting.sendProgressBarUpdate(this, 5, this.tile.getInvMode());
		crafting.sendProgressBarUpdate(this, 6, this.tile.getMatStock(0));
		crafting.sendProgressBarUpdate(this, 7, this.tile.getMatStock(1));
		crafting.sendProgressBarUpdate(this, 8, this.tile.getMatStock(2));
		crafting.sendProgressBarUpdate(this, 9, this.tile.getMatStock(3));
		crafting.sendProgressBarUpdate(this, 10, this.tile.getMatBuild(0));
		crafting.sendProgressBarUpdate(this, 11, this.tile.getMatBuild(1));
		crafting.sendProgressBarUpdate(this, 12, this.tile.getMatBuild(2));
		crafting.sendProgressBarUpdate(this, 13, this.tile.getMatBuild(3));
	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile.isUseableByPlayer(player);
	}
	
	/**��container�䴩shift�I���~���ʧ@
	 * shift�I�H���I�]�������~->�e����inventory, �I���inventory�������~->�e��H���I�]
	 * mergeItemStack: parm: item,start slot,end slot(���椣�P�w��J),�O�_�����hot bar
	 * slot id: 0:output 1~9:inventory 10~36:player inventory 37~45:hot bar
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
        ItemStack newStack = null;
        Slot slot = (Slot)this.inventorySlots.get(slotid);

        if(slot != null && slot.getHasStack()) { 	//�Yslot���F��
            ItemStack orgStack = slot.getStack();	//orgStack���o��slot���~
            newStack = orgStack.copy();				//newStack��orgStack�ƻs

            //�I��output slot��
            if(slotid == SLOT_OUTPUT) {
            	//�Noutput slot�����~���ո���inventory��slot�X��, ����X�֫h�Ǧ^null
            	if(!this.mergeItemStack(orgStack, SLOT_INVENTORY, SLOT_ALL, true)) 
            		return null;
            }  
            //�I��hot bar => ���ʨ�inventory or player inv
            else if (slotid > SLOT_HOTBAR) {
            	if(!this.mergeItemStack(orgStack, SLOT_INVENTORY, SLOT_HOTBAR, false))
            		return null;
            }
            //�I��player inv => ���ʨ�inventory or hot bar
            else if(slotid > SLOT_PLAYERINV && slotid < SLOT_HOTBAR) {
            	if(!this.mergeItemStack(orgStack, SLOT_INVENTORY, SLOT_PLAYERINV, true))
            		return null;
            } 
            //�I��inventory => ���ʨ�player inv or hot bar
            else {
            	if(!this.mergeItemStack(orgStack, SLOT_PLAYERINV, SLOT_ALL, false))
            		return null;
            }

            //�p�G���~���񧹤F, �h�]��null�M�ŸӪ��~
            if (orgStack.stackSize <= 0) {
                slot.putStack(null);
            }
            else { //�٨S��, ���]�@��slot update
                slot.onSlotChanged();
            }
        }
        return newStack;	//���~���ʧ���, �^�ǳѤU�����~
    }
	
	//�Ncontainer�ƭȸ�tile entity�����ƭȤ��, �p�G���P�h�o�e��s��client��gui�e�{�s�ƭ�
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

        for(Object crafter : this.crafters) {
            ICrafting icrafting = (ICrafting) crafter;

            if(this.guiPowerConsumed != this.tile.getPowerConsumed()) {  	//��sbuild�i�ױ�
                icrafting.sendProgressBarUpdate(this, 0, this.tile.getPowerConsumed());
                this.guiPowerConsumed = this.tile.getPowerConsumed();
            }
            
            if(this.guiPowerGoal != this.tile.getPowerGoal()) {  			//��sbuild�i�ױ�
                 icrafting.sendProgressBarUpdate(this, 1, this.tile.getPowerGoal());
                 this.guiPowerGoal = this.tile.getPowerGoal();
             }

            if(this.guiPowerRemained != this.tile.getPowerRemained()) {  	//��sfuel�s�q��
                icrafting.sendProgressBarUpdate(this, 2, this.tile.getPowerRemained());
                this.guiPowerRemained = this.tile.getPowerRemained();
            }
            
            if(this.guiBuildType != this.tile.getBuildType()) {  			//��s�سy����
                icrafting.sendProgressBarUpdate(this, 3, this.tile.getBuildType());
                this.guiBuildType = this.tile.getBuildType();
            }
            
            if(this.guiSelectMat != this.tile.getSelectMat()) {  			//��s������
                icrafting.sendProgressBarUpdate(this, 4, this.tile.getSelectMat());
                this.guiSelectMat = this.tile.getSelectMat();
            }
            
            if(this.guiInvMode != this.tile.getInvMode()) {  			//��sinv mode
                icrafting.sendProgressBarUpdate(this, 5, this.tile.getInvMode());
                this.guiInvMode = this.tile.getInvMode();
            }
            
            for(int i = 0; i < this.guiMatStock.length; i++) {
            	if(this.guiMatStock[i] != this.tile.getMatStock(i)) {  	//��s����s�q
                    icrafting.sendProgressBarUpdate(this, i+6, this.tile.getMatStock(i));
                    this.guiMatStock[i] = this.tile.getMatStock(i);
                }
            }
            
            for(int i = 0; i < this.guiMatBuild.length; i++) {
            	if(this.guiMatBuild[i] != this.tile.getMatBuild(i)) {  	//��s����سy�q
                    icrafting.sendProgressBarUpdate(this, i+10, this.tile.getMatBuild(i));
                    this.guiMatBuild[i] = this.tile.getMatBuild(i);
                }
            }
        }
        
    }

	//client��container�����s��
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int valueType, int updatedValue) {
        
		switch(valueType) {
		case 0: 
			this.tile.setPowerConsumed(updatedValue);
			break;
		case 1:
			this.tile.setPowerGoal(updatedValue);
			break;
		case 2:
			this.tile.setPowerRemained(updatedValue);
			break;
		case 3:
			this.tile.setBuildType(updatedValue);
			break;
		case 4:
			this.tile.setSelectMat(updatedValue);
			break;
		case 5:
			this.tile.setInvMode(updatedValue);
			break;
		case 6:
			this.tile.setMatStock(0, updatedValue);
			break;
		case 7:
			this.tile.setMatStock(1, updatedValue);
			break;
		case 8:
			this.tile.setMatStock(2, updatedValue);
			break;
		case 9:
			this.tile.setMatStock(3, updatedValue);
			break;
		case 10:
			this.tile.setMatBuild(0, updatedValue);
			break;
		case 11:
			this.tile.setMatBuild(1, updatedValue);
			break;
		case 12:
			this.tile.setMatBuild(2, updatedValue);
			break;
		case 13:
			this.tile.setMatBuild(3, updatedValue);
			break;
		}
    }

	
}

