package com.lulan.shincolle.client.gui.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.item.ItemStack;

import com.lulan.shincolle.tileentity.TileEntityDesk;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**SLOT POSITION
 * S1:grudge(33,29) S2:abyssium(53,29) S3:ammo(73,29) S4:poly(93,29)
 * fuel(8,53) fuel bar(10,48 height=30) fuel color bar(176,46)
 * ship button(123,17) equip button(143,17)
 * output(134,44) player inv(8,87) action bar(8,145)
 */
public class ContainerDesk extends Container {
	
	private TileEntityDesk tile;
	private int guiFunc, bookChap, bookPage;
	
	
	public ContainerDesk(InventoryPlayer invPlayer, TileEntityDesk te) {
		this.tile = te;

	}

	//���a�O�_�i�HĲ�o�k���I����ƥ�
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile.isUseableByPlayer(player);
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
		crafting.sendProgressBarUpdate(this, 1, this.tile.guiFunc);
		crafting.sendProgressBarUpdate(this, 2, this.tile.book_chap);
		crafting.sendProgressBarUpdate(this, 3, this.tile.book_page);
	}
	
	//�Ncontainer�ƭȸ�tile entity�����ƭȤ��, �p�G���P�h�o�e��s��client��gui�e�{�s�ƭ�
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

//        for(Object crafter : this.crafters) {
//            ICrafting icrafting = (ICrafting) crafter;
//            
//            if(this.guiFunc != this.tile.guiFunc ||
//               this.bookChap != this.tile.book_chap ||
//               this.bookPage != this.tile.book_page) {
//                icrafting.sendProgressBarUpdate(this, 1, this.tile.guiFunc);
//                icrafting.sendProgressBarUpdate(this, 2, this.tile.book_chap);
//                icrafting.sendProgressBarUpdate(this, 3, this.tile.book_page);
//                this.guiFunc = this.tile.guiFunc;
//                this.bookChap = this.tile.book_chap;
//                this.bookPage = this.tile.book_page;
//                this.tile.sendSyncPacket();
//            }
//        }
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
		}
    }

	
}

