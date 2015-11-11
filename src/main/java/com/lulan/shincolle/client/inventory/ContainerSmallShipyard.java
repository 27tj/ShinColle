package com.lulan.shincolle.client.inventory;

import com.lulan.shincolle.crafting.SmallRecipes;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**SLOT POSITION
 * S1:grudge(33,29) S2:abyssium(53,29) S3:ammo(73,29) S4:poly(93,29)
 * fuel(8,53) fuel bar(10,48 height=30) fuel color bar(176,46)
 * ship button(123,17) equip button(143,17)
 * output(134,44) player inv(8,87) action bar(8,145)
 */
public class ContainerSmallShipyard extends Container {
	
	private TileEntitySmallShipyard te;
	public int guiBuildType, guiConsumedPower, guiRemainedPower, guiGoalPower;
	
	
	public ContainerSmallShipyard(InventoryPlayer invPlayer, TileEntitySmallShipyard teSmallShipyard) {
		this.te = teSmallShipyard;
			
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 0, 33, 29));  //grudge
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 1, 53, 29));  //abyssium
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 2, 73, 29));  //ammo
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 3, 93, 29));  //poly
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 4, 8, 53));   //fuel
		this.addSlotToContainer(new SlotSmallShipyard(teSmallShipyard, 5, 134, 44)); //output
		
		//player inventory
		for(int i=0; i<3; i++) {
			for(int j=0; j<9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j+i*9+9, 8+j*18, 87+i*18));
			}
		}
		
		//player action bar (hot bar)
		for(int i=0; i<9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8+i*18, 145));
		}
	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return te.isUseableByPlayer(player);
	}
	
	/**��container�䴩shift�I���~���ʧ@, ����ContainerFurnace�������ƻs�L�ӭק�
	 * shift�I�H���I�]�������~->�P�w���~�����e����w��l, �Icontainer�������~->�e��H���I�]
	 * mergeItemStack: parm: item,start slot,end slot(���椣�P�w��J),�O�_�����hot bar
	 * slot id: 0:grudge 1:abyssium 2:ammo 3:polymetal 4:fuel 5:output
	 *          6~32:player inventory 33~41:hot bar
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
        ItemStack itemstack = null;
        Slot slot = (Slot)this.inventorySlots.get(slotid);
        int itemID = -1;

        if(slot != null && slot.getHasStack()) { 			//�Yslot���F��
            ItemStack itemstack1 = slot.getStack();			//itemstack1���o��slot���~
            itemstack = itemstack1.copy();					//itemstack�ƻs�@��itemstack1
            itemID = SmallRecipes.getMaterialType(itemstack1);//���o���~����(����slot id)

            if(slotid == 5) {  //�Y�I��output slot��
            	//�Noutput slot�����~���ո�player inventory or hot bar��slot�X��, ����X�֫h�Ǧ^null
            	if(!this.mergeItemStack(itemstack1, 6, 42, true)) {
                	return null;
                }
                slot.onSlotChange(itemstack1, itemstack); //�Y���~���\�h�ʹL, �h�I�sslot change�ƥ�
            }
            //�p�G�O�I��player inventory slot��, �P�w�O�_���iinput slot
            else if(slotid > 5) {        	          	
            	if(itemID >= 0) {  //item ID: -1:other 0~3:material 4:fuel
            		if(!this.mergeItemStack(itemstack1, itemID, itemID+1, false)) { //���ն�islot 0~4��
            			return null;
                    }
                }
            	//�Y���~���O�i�Χ���, �B���~�bplayer inventory, �h����hot bar
                else if (slotid > 5 && slotid < 33) {
                	if (!this.mergeItemStack(itemstack1, 33, 42, false)) {
                        return null;
                    }
                }
            	//�Y���~���O�i�Χ���, �B���~�bhot bar, �h����player inventory
                else if (slotid > 32 && !this.mergeItemStack(itemstack1, 6, 33, false)) {
                    return null;
                }
            }
            //�p�G�O�I��slot 0~4, �h����player inventory or hot bar
            else if (!this.mergeItemStack(itemstack1, 6, 42, true)) {
                return null;
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
		crafting.sendProgressBarUpdate(this, 0, this.te.getBuildType());		//�سy����
	}
	
	//�Ncontainer�ƭȸ�tile entity�����ƭȤ��, �p�G���P�h�o�e��s��client��gui�e�{�s�ƭ�
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

        for(Object crafter : this.crafters) {
            ICrafting icrafting = (ICrafting) crafter;
            
            if(this.guiBuildType != this.te.getBuildType()) {  			//��s�سy����
                icrafting.sendProgressBarUpdate(this, 0, this.te.getBuildType());
                this.guiBuildType = this.te.getBuildType();
            }
            
            if(this.guiConsumedPower != this.te.getPowerConsumed() ||
               this.guiRemainedPower != this.te.getPowerRemained() ||
               this.guiGoalPower != this.te.getPowerGoal()) {
    			this.te.sendSyncPacket();
                this.guiConsumedPower = this.te.getPowerConsumed();
                this.guiRemainedPower = this.te.getPowerRemained();
                this.guiGoalPower = this.te.getPowerGoal();
                
                //��sendProgressBarUpdate��@update��flag, ���O���q�o��ǹ�ڭ�, �ӬO�t�~�Φۭq�ʥ]��
                icrafting.sendProgressBarUpdate(this, 1, 0);
            }
        } 
    }

	//client��container�����s��
	@Override
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int valueType, int updatedValue) {
		World world = Minecraft.getMinecraft().theWorld;
		//����client�ݪ�tile, �`�N��쪺tile��container����tile�D�ۦP�ƥ�(����server�ݰƥ�, �Y�ϼ�CLIENT�]���Oclient��)
		TileEntitySmallShipyard tile = (TileEntitySmallShipyard) world.getTileEntity(this.te.xCoord, this.te.yCoord, this.te.zCoord);
		
		
		switch(valueType) {
		case 0:
			this.te.setBuildType(updatedValue);
			break;
		case 1:
			if(tile != null) {
				this.te.setPowerConsumed(tile.getPowerConsumed());
				this.te.setPowerRemained(tile.getPowerRemained());
				this.te.setPowerGoal(tile.getPowerGoal());
			}
			break;
		}
    }

	
}
