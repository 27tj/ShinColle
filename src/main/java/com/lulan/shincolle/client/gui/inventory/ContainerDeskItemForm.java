package com.lulan.shincolle.client.gui.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.lulan.shincolle.entity.ExtendPlayerProps;

/**SLOT POSITION
 * no slot
 */
public class ContainerDeskItemForm extends Container {
	
	private InventoryPlayer playerInv;
	private EntityPlayer player;
	private ExtendPlayerProps extProps;
	private int bookChap, bookPage;
	
	
	public ContainerDeskItemForm(InventoryPlayer invPlayer, EntityPlayer player) {
		this.playerInv = invPlayer;
		this.player = player;
		this.extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		
		//server side flag
		if(this.extProps != null) {
			this.extProps.setIsOpeningGUI(true);
		}
	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}
	
	/** Called when the container is closed */
    public void onContainerClosed(EntityPlayer player) {
    	ExtendPlayerProps props = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
    	
    	//server side flag
		if(props != null) {
			props.setIsOpeningGUI(false);
		}
    }
	
	/**��container�䴩shift�I���~���ʧ@, ����ContainerFurnace�������ƻs�L�ӭק�
	 * shift�I�H���I�]�������~->�P�w���~�����e����w��l, �Icontainer�������~->�e��H���I�]
	 * mergeItemStack: parm: item,start slot,end slot(���椣�P�w��J),�O�_�����hot bar
	 *        
	 * slot id: 0~26:player inventory  27~35:hot bar        
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
        return null;
    }
	
//	//�o�egui��s, �C���}�Ҥ���ɩI�s�@�� 
//	@Override
//	public void addCraftingToCrafters (ICrafting crafting) {
//		super.addCraftingToCrafters(crafting);
//	}
	
//	//�Ncontainer�ƭȸ�tile entity�����ƭȤ��, �p�G���P�h�o�e��s��client��gui�e�{�s�ƭ�
//	@Override
//	public void detectAndSendChanges() {
//		super.detectAndSendChanges();
//    }

//	//client��container�����s��
//	@Override
//	@SideOnly(Side.CLIENT)
//    public void updateProgressBar(int valueType, int updatedValue) {
//		switch(valueType) {
//		case 2:
//			this.tile.book_chap = updatedValue;
//			break;
//		}
//    }

	
}


