package com.lulan.shincolle.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.lulan.shincolle.network.C2SGUIPackets;
import com.lulan.shincolle.network.S2CGUIPackets;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;

public class TileEntityDesk extends BasicTileEntity {

	public int guiFunc = 0;
	public int book_chap = 0;
	public int book_page = 0;
	
	
	public TileEntityDesk() {
	}
	
	@Override
	public int getFuelSlotMin() {
		return 0;
	}

	@Override
	public int getFuelSlotMax() {
		return 0;
	}

	//GUI��ܪ��W��, ��custom name�h��, ���M�N�ιw�]�W��
	@Override
	public String getInventoryName() {
		return this.hasCustomInventoryName() ? this.customName : "container."+Reference.MOD_ID+":BlockDesk";
	}
	
	//�O�_�i�H�k���I�}���
	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		//�ѩ�|���h��tile entity�ƥ�, �n���T�{�y�ЬۦP���ƥ��~��ϥ�
		if (worldObj.getTileEntity(xCoord, yCoord, zCoord) != this) {
			return false;
		}
		else {	//�T�{player�n�b��tile entity 8�椺, �H�K�W�XŪ���d�� or ���ͨ�L����bug
			return player.getDistanceSq(xCoord+0.5D, yCoord+0.5D, zCoord+0.5D) <= 64;
		}
	}
	
	//Ū��nbt���
	@Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);	//�qnbtŪ�������xyz�y��
        
        guiFunc = compound.getInteger("guiFunc");
        book_chap = compound.getInteger("bookChap");
        book_page = compound.getInteger("bookPage");
    }
	
	//�N��Ƽg�inbt
	@Override
	public void writeToNBT(NBTTagCompound compound) {
		super.writeToNBT(compound);
		
		compound.setInteger("guiFunc", guiFunc);
		compound.setInteger("bookChap", book_chap);
		compound.setInteger("bookPage", book_page);
	}
	
	@Override
	public void sendSyncPacketC2S() {
		if(this.worldObj.isRemote) {
			int[] data = new int[3];
			data[0] = this.guiFunc;
			data[1] = this.book_chap;
			data[2] = this.book_page;
			CommonProxy.channelG.sendToServer(new C2SGUIPackets(this, ID.B.Desk_Sync, data));
		}
	}
	
	@Override
	public void sendSyncPacket() {
		if(!this.worldObj.isRemote) {
			LogHelper.info("DEBUG : desk sync s2c "+this.guiFunc+" "+this.book_chap+" "+this.book_page);
			TargetPoint point = new TargetPoint(this.worldObj.provider.dimensionId, this.xCoord, this.yCoord, this.zCoord, 8D);
			CommonProxy.channelG.sendToAllAround(new S2CGUIPackets(this), point);
		}
	}
	
	public void setSyncData(int[] data) {
		if(data != null) {
//			LogHelper.info("DEBUG : desk sync: "+data[1]);
			this.guiFunc = data[0];
			this.book_chap = data[1];
			this.book_page = data[2];
		}
	}
	

}
