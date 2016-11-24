package com.lulan.shincolle.client.gui.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import com.lulan.shincolle.tileentity.TileEntityVolCore;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class ContainerVolCore extends Container {
	
	//for shift item
	private static final int SLOT_INVENTORY = 0;
	private static final int SLOT_PLAYERINV = 9;
	private static final int SLOT_HOTBAR = 36;
	private static final int SLOT_ALL = 45;
		
	private TileEntityVolCore tile;
	public int guiRemainedPower;
	
	
	public ContainerVolCore(InventoryPlayer invPlayer, TileEntityVolCore tile) {
		this.tile = tile;

		//tile inventory
		this.addSlotToContainer(new SlotVolCore(tile, 0, 62, 19));
		this.addSlotToContainer(new SlotVolCore(tile, 1, 80, 19));
		this.addSlotToContainer(new SlotVolCore(tile, 2, 98, 19));
		this.addSlotToContainer(new SlotVolCore(tile, 3, 62, 37));
		this.addSlotToContainer(new SlotVolCore(tile, 4, 80, 37));
		this.addSlotToContainer(new SlotVolCore(tile, 5, 98, 37));
		this.addSlotToContainer(new SlotVolCore(tile, 6, 62, 55));
		this.addSlotToContainer(new SlotVolCore(tile, 7, 80, 55));
		this.addSlotToContainer(new SlotVolCore(tile, 8, 98, 55));
		
		//player inventory
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 9; j++) {
				this.addSlotToContainer(new Slot(invPlayer, j+i*9+9, 8+j*18, 84+i*18));
			}
		}
		
		//player hot bar
		for(int i = 0; i < 9; i++) {
			this.addSlotToContainer(new Slot(invPlayer, i, 8+i*18, 142));
		}
	}

	//玩家是否可以觸發右鍵點方塊事件
	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return tile.isUseableByPlayer(player);
	}
	
	/**使container支援shift點物品的動作, 此為ContainerFurnace中直接複製過來修改
	 * shift點人物背包中的物品->判定物品類型送到指定格子, 點container中的物品->送到人物背包
	 * mergeItemStack: parm: item,start slot,end slot(此格不判定放入),是否先放到hot bar
	 * slot id: 0:grudge 1:abyssium 2:ammo 3:polymetal 4:fuel 5:output
	 *          6~32:player inventory 33~41:hot bar
	 */
	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotid) {
		ItemStack newStack = null;
        Slot slot = (Slot)this.inventorySlots.get(slotid);

        if(slot != null && slot.getHasStack()) { 	//若slot有東西
            ItemStack orgStack = slot.getStack();	//orgStack取得該slot物品
            newStack = orgStack.copy();				//newStack為orgStack複製

            //點擊hot bar => 移動到inventory or player inv
            if(slotid >= SLOT_HOTBAR) {
            	if(!this.mergeItemStack(orgStack, SLOT_INVENTORY, SLOT_HOTBAR, false))
            		return null;
            }
            //點擊player inv => 移動到inventory or hot bar
            else if(slotid >= SLOT_PLAYERINV) {
            	if(!this.mergeItemStack(orgStack, SLOT_INVENTORY, SLOT_PLAYERINV, true))
            		return null;
            } 
            //點擊inventory => 移動到player inv or hot bar
            else {
            	if(!this.mergeItemStack(orgStack, SLOT_PLAYERINV, SLOT_ALL, false))
            		return null;
            }

            //如果物品都放完了, 則設成null清空該物品
            if(orgStack.stackSize <= 0) {
                slot.putStack(null);
            }
            else { //還沒放完, 先跑一次slot update
                slot.onSlotChanged();
            }
        }

        return newStack;	//物品移動完成, 回傳剩下的物品
    }
	
	//發送更新gui進度條更新, 比detectAndSendChanges還要優先
	@Override
	public void addCraftingToCrafters (ICrafting crafting) {
		super.addCraftingToCrafters(crafting);
//		crafting.sendProgressBarUpdate(this, 0, this.te.getBuildType());		//建造類型
	}
	
	//將container數值跟tile entity內的數值比對, 如果不同則發送更新給client使gui呈現新數值
	@Override
	public void detectAndSendChanges() {
		super.detectAndSendChanges();

        for(Object crafter : this.crafters) {
            ICrafting icrafting = (ICrafting) crafter;
            
            if(this.guiRemainedPower != this.tile.getPowerRemained()) {
    			this.tile.sendSyncPacket();
                this.guiRemainedPower = this.tile.getPowerRemained();
                
                //用sendProgressBarUpdate當作update的flag, 但是不從這邊傳實際值, 而是另外用自訂封包傳
                icrafting.sendProgressBarUpdate(this, 1, 0);
            }
        } 
    }

	//client端container接收新值
	@Override
	@SideOnly(Side.CLIENT)
    public void updateProgressBar(int valueType, int updatedValue) {
		World world = Minecraft.getMinecraft().theWorld;
		TileEntityVolCore tile = (TileEntityVolCore) world.getTileEntity(this.tile.xCoord, this.tile.yCoord, this.tile.zCoord);
		
		switch(valueType) {
		case 1:
			if(tile != null) {
				this.tile.setPowerRemained(tile.getPowerRemained());
			}
			break;
		}
    }

	
}

