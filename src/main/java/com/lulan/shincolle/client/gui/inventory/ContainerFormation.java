package com.lulan.shincolle.client.gui.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.lulan.shincolle.entity.ExtendPlayerProps;

/**SLOT POSITION
 * no slot
 */
public class ContainerFormation extends Container {
	
	private InventoryPlayer playerInv;
	private EntityPlayer player;
	private ExtendPlayerProps extProps;
	
	
	public ContainerFormation(InventoryPlayer invPlayer, EntityPlayer player) {
		this.playerInv = invPlayer;
		this.player = player;
		this.extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		
	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}
	
	/** shift�I���~���ʧ@ */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
        return null;
    }

	
}



