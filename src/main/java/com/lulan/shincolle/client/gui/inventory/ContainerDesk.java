package com.lulan.shincolle.client.gui.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;

import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**SLOT POSITION
 * no slot
 */
public class ContainerDesk extends Container {
	
	private TileEntityDesk tile;
	private InventoryPlayer playerInv;
	private EntityPlayer player;
	private ExtendPlayerProps extProps;
	private int guiFunc, bookChap, bookPage, allyCD, type;
	
	
	public ContainerDesk(InventoryPlayer invPlayer, TileEntityDesk te, EntityPlayer player, int type) {
		this.playerInv = invPlayer;
		this.tile = te;
		this.player = player;
		this.extProps = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
		this.type = type;
		
		//server side flag
		if(this.extProps != null) {
			this.extProps.setOpeningGUI(true);
		}
	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		//open GUI with TileEntity
		if(type == 0) {
			return tile.isUseableByPlayer(player);
		}
		//open GUI with item
		return true;
	}
	
	/** Called when the container is closed, used for team list sync */
    public void onContainerClosed(EntityPlayer player) {
    	ExtendPlayerProps props = (ExtendPlayerProps) player.getExtendedProperties(ExtendPlayerProps.PLAYER_EXTPROP_NAME);
    	
    	//server side flag
		if(props != null) {
			props.setOpeningGUI(false);
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
	
	//�o�egui��s, �C���}�Ҥ���ɩI�s�@�� 
	@Override
	public void addCraftingToCrafters (ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
		if(type == 0) {
			crafting.sendProgressBarUpdate(this, 1, this.tile.guiFunc);
			crafting.sendProgressBarUpdate(this, 2, this.tile.book_chap);
			crafting.sendProgressBarUpdate(this, 3, this.tile.book_page);
			crafting.sendProgressBarUpdate(this, 4, this.tile.radar_zoomLv);
			crafting.sendProgressBarUpdate(this, 5, this.extProps.getPlayerTeamCooldownInSec());
		}
	}
	
	//�Ncontainer�ƭȸ�tile entity�����ƭȤ��, �p�G���P�h�o�e��s��client��gui�e�{�s�ƭ�
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();
		if(type == 0) {	
	        for(Object crafter : this.crafters) {
	            ICrafting icrafting = (ICrafting) crafter;
	            
	            if(this.allyCD != this.extProps.getPlayerTeamCooldownInSec()) {
	                icrafting.sendProgressBarUpdate(this, 5, this.extProps.getPlayerTeamCooldownInSec());
	                this.allyCD = this.extProps.getPlayerTeamCooldownInSec();
	            }
	        }
		}
    }

	//client��container�����s��
	@Override
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int valueType, int updatedValue) {
		switch(valueType) {
		case 1:
			this.tile.guiFunc = updatedValue;
			break;
		case 2:
			this.tile.book_chap = updatedValue;
			break;
		case 3:
			this.tile.book_page = updatedValue;
			break;
		case 4:
			this.tile.radar_zoomLv = updatedValue;
			break;
		case 5:
			this.extProps.setPlayerTeamCooldown(updatedValue * 20);  //second to tick
			break;
		}
    }

	
}

