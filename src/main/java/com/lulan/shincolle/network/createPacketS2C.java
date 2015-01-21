package com.lulan.shincolle.network;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;

import java.io.IOException;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.Names;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.entity.Entity;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;

//create server packet by Jabelar
//web: jabelarminecraft.blogspot.tw/p/packet-handling-for-minecraft-forge-172.html
public class createPacketS2C {
 
	public createPacketS2C() {
	}

	/**ENTITY SYNC PACKET <br>
	 * �Ω�P�Bserver��client��entity��� <br>
	 * Format: PacketID + EntityID + ShipLevel + Kills + 
	 *         AttrBonus[] + AttrFinal[] + EntityState[]
	 * 
	 */
	public static FMLProxyPacket createEntityPacket(Entity parEntity) throws IOException {
		LogHelper.info("Sending Entity Sync Packet on Server Side");
		//�إ�packet�ǿ�stream
		ByteBufOutputStream bbos = new ByteBufOutputStream(Unpooled.buffer());
		
		//Packet ID (�|��b�ʥ]�Y�H���ѫʥ]����)
		bbos.writeByte(Names.Packets.ENTITY_SYNC);
		//Entity ID (�Ω����entity�O���@��)
		bbos.writeInt(parEntity.getEntityId());
		//�H�U�g�J�n�ǰe�����
		if (parEntity instanceof BasicEntityShip) {
			BasicEntityShip entity = (BasicEntityShip)parEntity;
			bbos.writeShort(entity.ShipLevel);
			bbos.writeInt(entity.Kills);
			
			bbos.writeShort(entity.AttrBonusShort[AttrID.HP]);
			bbos.writeShort(entity.AttrBonusShort[AttrID.ATK]);
			bbos.writeShort(entity.AttrBonusShort[AttrID.DEF]);
			bbos.writeFloat(entity.AttrBonusFloat[AttrID.SPD]);
			bbos.writeFloat(entity.AttrBonusFloat[AttrID.MOV]);
			bbos.writeFloat(entity.AttrBonusFloat[AttrID.HIT]);
			
			bbos.writeShort(entity.AttrFinalShort[AttrID.HP]);
			bbos.writeShort(entity.AttrFinalShort[AttrID.ATK]);
			bbos.writeShort(entity.AttrFinalShort[AttrID.DEF]);
			bbos.writeFloat(entity.AttrFinalFloat[AttrID.SPD]);
			bbos.writeFloat(entity.AttrFinalFloat[AttrID.MOV]);
			bbos.writeFloat(entity.AttrFinalFloat[AttrID.HIT]);
			
			bbos.writeByte(entity.EntityState[AttrID.State]);
			bbos.writeByte(entity.EntityState[AttrID.Emotion]);
			bbos.writeByte(entity.EntityState[AttrID.SwimType]);
		}

		// put payload into a packet  
		FMLProxyPacket thePacket = new FMLProxyPacket(bbos.buffer(), CommonProxy.channelName);
		// don't forget to close stream to avoid memory leak
		bbos.close();
  
		return thePacket;
	}
 
	// send to all player on the server
	public static void sendToAll(FMLProxyPacket parPacket) {
      CommonProxy.channel.sendToAll(parPacket);
	}

	public static void sendS2CEntitySync(Entity parEntity) {
    	try {
    		sendToAll(createEntityPacket(parEntity));
    	} 
    	catch (IOException e) {
    		e.printStackTrace();
    	}
	}
}
