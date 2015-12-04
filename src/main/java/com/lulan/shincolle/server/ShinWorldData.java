package com.lulan.shincolle.server;

import java.util.Iterator;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.WorldSavedData;

import com.lulan.shincolle.proxy.ServerProxy;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

/**���A���ݸ��
 * �x�splayer id��, ���A���ݧP�w�θ��
 * ��class�ΨӳB�zServerProxy��MapStorage��������Ʀs���ʧ@
 *
 * tut tag: diesieben07, worldsaveddata
 */
public class ShinWorldData extends WorldSavedData {

	public static final String SAVEID = Reference.MOD_ID;
	public static final String TAG_NEXTPLAYERID = "nextPlayerID";
	public static final String TAG_NEXTSHIPID = "nextShipID";
	public static final String TAG_NEXTTEAMID = "nextTeamID";
	public static final String TAG_PLAYERDATA = "playerData";
	public static final String TAG_PUID = "pUID";
	public static final String TAG_PDATA = "pData";
	
	//data
	public static NBTTagCompound nbtData;

	
	public ShinWorldData() {
		super(SAVEID);
	}
	
	public ShinWorldData(String saveid) {
		super(saveid);
	}

	/**read server save file
	 * from save file to ServerProxy map
	 */
	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		//get data
		nbtData = (NBTTagCompound) nbt.copy();
//		LogHelper.info("DEBUG : world data: load NBT: "+nbtData.toString());
	}//end read nbt

	/**write server save file
	 * from ServerProxy map to save file
	 */
	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		//save common variable
		nbt.setInteger(TAG_NEXTPLAYERID, ServerProxy.getNextPlayerID());
		nbt.setInteger(TAG_NEXTSHIPID, ServerProxy.getNextShipID());
		nbt.setInteger(TAG_NEXTTEAMID, ServerProxy.getNextTeamID());
		
		//save player data:  from playerMap to server save file
		NBTTagList list = new NBTTagList();
		Iterator iter = ServerProxy.getAllPlayerWorldData().entrySet().iterator();
		
		while(iter.hasNext()) {
		    Map.Entry entry = (Map.Entry) iter.next();
		    int uid = (Integer) entry.getKey();
		    int[] data = (int[]) entry.getValue();
		    
		    NBTTagCompound save = new NBTTagCompound();
		    save.setInteger(TAG_PUID, uid);
		    save.setIntArray(TAG_PDATA, data);
		    list.appendTag(save);	//�Nsave�[�J��list��, ���ˬd�O�_�����ƪ�tag, �ӬO�s�W�@��tag
		} 
//		LogHelper.info("DEBUG : world data: save NBT: "+list.tagCount());
		nbt.setTag(TAG_PLAYERDATA, list);	//�Nlist�[�J��nbt��
	}//end write nbt

}
