package com.lulan.shincolle.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;


//��class�w��extend entity prop�X��, �ݧR����
public class ShipInventory implements IInventory {

	//0~5:equip 6~23:inventory
	private ItemStack[] slots = new ItemStack[ContainerShipInventory.SLOTS_TOTAL];		
	private static final String tagName = "ShipInv";	//ship inventory nbt tag
	
	
	@Override
	public int getSizeInventory() {
		return slots.length;
	}

	@Override
	public ItemStack getStackInSlot(int i) {
		return slots[i];
	}

	@Override
	public ItemStack decrStackSize(int i, int j) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            if (itemStack.stackSize <= j) {			  //�Y�ƶq<=j��
                setInventorySlotContents(i, null);	  //�h��slot�M��
            }
            else {									  //�Y�ƶq >j��
                itemStack = itemStack.splitStack(j);  //��itemstack�ƶq-j
                if (itemStack.stackSize == 0) {
                    setInventorySlotContents(i, null);//��������, slot�M��
                }
            }
        }
        return itemStack;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int i) {
		ItemStack itemStack = getStackInSlot(i);
        if (itemStack != null) {
            setInventorySlotContents(i, null);
        }
        return itemStack;
	}

	@Override
	public void setInventorySlotContents(int i, ItemStack itemstack) {
		slots[i] = itemstack;
		//�Y��W���~�W�L�Ӯ�l����ƶq, �h�u���i����ƶq
		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
			itemstack.stackSize = getInventoryStackLimit();
		}		
	}

	@Override
	public String getInventoryName() {
		return "Ship Inventory";
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void markDirty() {}

	@Override
	public boolean isUseableByPlayer(EntityPlayer p_70300_1_) {
		return true;
	}

	@Override
	public void openInventory() {}

	@Override
	public void closeInventory() {}

	//check item valid in CUSTOM SLOT CLASS, not here
	@Override
	public boolean isItemValidForSlot(int p_94041_1_, ItemStack p_94041_2_) {
		return true;
	}
	
	//Ū��nbt data�s��item���
	public void readFromNBT(NBTTagCompound compound) {
		NBTTagList list = compound.getTagList(tagName, 10);

		for(int i=0; i<list.tagCount(); i++) {
			NBTTagCompound item = (NBTTagCompound) list.getCompoundTagAt(i);
			byte sid = item.getByte("Slot");

			if (sid>=0 && sid<slots.length) {
				slots[sid] = ItemStack.loadItemStackFromNBT(item);
			}
		}
	}
	
	//�Nitem��Ƽg�inbt
	public void writeToNBT(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();

		for(int i=0; i<slots.length; i++) {
			if (slots[i] != null) {
				NBTTagCompound item = new NBTTagCompound();
				item.setByte("Slot", (byte)i);
				slots[i].writeToNBT(item);
				list.appendTag(item);
			}
		}
		
		compound.setTag(tagName, list);	//slot��ƥ����s�btag: ShipInv��
	}


}
