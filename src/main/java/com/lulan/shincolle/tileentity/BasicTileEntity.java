package com.lulan.shincolle.tileentity;

import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class BasicTileEntity extends TileEntity implements ISidedInventory {
	
	protected ItemStack slots[];
	protected String customName;
	protected int syncTime;		//for sync
	
	public BasicTileEntity() {
        customName = "";
    }
	
	public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
    public boolean hasCustomName() {
        return customName != null && customName.length() > 0;
    }
    
    //�T�{�O�_���ۭq�W��(�ϥ�name tag���W�L����)
  	@Override
  	public boolean hasCustomInventoryName() {
  		return this.customName != null && this.customName.length() > 0;
  	}
    
  	@Override
  	public int getSizeInventory() {
  		return slots.length;
  	}

  	@Override
  	public ItemStack getStackInSlot(int i) {
  		return slots[i];
  	}
  	
    //����slot i��, �ƶqj�Ӫ��~, �^�Ǭ�itemstack, ���k�䵥�ʧ@�s��slot�ɷ|�I�s����k
  	//(�Dshift�ʧ@) shift�ʧ@�bcontainer����transferStackInSlot����@
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
  	
    //����gui�ɬO�_���Xslot�������~, �H�K�����~�����X��, �Ω�X���x����� (������S���Ψ�)
  	@Override
  	public ItemStack getStackInSlotOnClosing(int i) {
//  		ItemStack itemStack = getStackInSlot(i);
//          if (itemStack != null) {
//              setInventorySlotContents(i, null);
//          }
//          return itemStack;
  		return null;
  	}
  	
    //�Nslot�]���ؼ�itemstack(�]�i�H�]��null) �Ω�decrStackSize����k
  	@Override
  	public void setInventorySlotContents(int i, ItemStack itemstack) {
  		slots[i] = itemstack;
  		//�Y��W���~�W�L�Ӯ�l����ƶq, �h�u���i����ƶq
  		if (itemstack != null && itemstack.stackSize > getInventoryStackLimit()) {
  			itemstack.stackSize = getInventoryStackLimit();
  		}	
  	}
  	
    //�C��i�񪺳̤j�ƶq�W��
  	@Override
  	public int getInventoryStackLimit() {
  		return 64;
  	}
  	
    //�����k�����container�å�
  	public void openInventory() {}
  	public void closeInventory() {}
  	
    //�ϥκ޽u/�|���J�ɩI�s, ���A�Ω��ʸm�J
  	@Override
  	public boolean canInsertItem(int slot, ItemStack itemstack, int side) {
  		return this.isItemValidForSlot(slot, itemstack);  //����side �ҥΦ��P�w
  	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return null;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack itemstack, int side) {
		return false;
	}

	@Override
	public String getInventoryName() {
		return null;
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack itemstack) {
		return false;
	}
	
	//sync data for GUI display
	public void sendSyncPacket() {
		if(!this.worldObj.isRemote) {
			TargetPoint point = new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 8D);
			CommonProxy.channelG.sendToAllAround(new S2CGUIPackets(this), point);
		}
	}

}
