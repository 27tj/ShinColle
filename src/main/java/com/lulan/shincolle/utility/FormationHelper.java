package com.lulan.shincolle.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.entity.ExtendPlayerProps;
import com.lulan.shincolle.network.S2CEntitySync;
import com.lulan.shincolle.proxy.CommonProxy;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.reference.Values;

/** FORMATION HELPER
 * 
 *  formation process:
 *  1. pointer GUI set formation (client to server)
 *  2. server apply formation id to team (sync data to client)
 *  3. ship update formation buff (calc new attrs and sync data to client)
 *     slow update: check every X ticks
 *     fast update: check update flag every Y ticks 
 */
public class FormationHelper {

	
	public FormationHelper() {}
	
	/** set current team formation id */
	public static void setFormationID(ExtendPlayerProps props, int formatID) {
		if(props != null) {
			setFormationID(props, props.getPointerTeamID(), formatID);
		}
	}
	
	/** set team formation id */
	public static void setFormationID(ExtendPlayerProps props, int teamID, int formatID) {
		if(props != null) {
			int num = props.getNumberOfShip(teamID);
			
			if(num > 4 && formatID > 0) {	//can apply formation
				setFormationForShip(props, teamID, formatID);
				props.setFormatID(teamID, formatID);
				
				//update formation guard position
				BasicEntityShip[] ships = props.getShipEntityByMode(2);
				FormationHelper.applyFormationMoving(ships, formatID);
			}
			else {
				setFormationForShip(props, teamID, 0);
				props.setFormatID(teamID, 0);
			}
			
			//sync formation data
			props.sendSyncPacket(1);
		}
	}
	
	/** set formation id and pos to ship */
	public static void setFormationForShip(ExtendPlayerProps props, int teamID, int formatID) {
		BasicEntityShip ship = null;
		float buffMOV = getFormationBuffValue(formatID, 0)[ID.Formation.MOV];
		float maxMOV = 1000F;
		float temp = 0F;
		
		//set buff value to ship
		for(int i = 0; i < 6; i++) {
			ship = props.getShipEntity(teamID, i);
			
			if(ship != null) {
				ship.setUpdateFlag(ID.FU.FormationBuff, true);  //set update
				
				temp = ship.getStateFinalBU(ID.MOV);			//get MIN moving speed in team
				if(temp < maxMOV) maxMOV = temp;
			}
		}
		
		//apply same moving speed to all ships in team
		maxMOV += buffMOV;  //moving speed for formation
		
		for(int j = 0; j < 6; j++) {
			ship = props.getShipEntity(teamID, j);
			
			if(ship != null) {
				ship.setEffectFormationFixed(ID.FormationFixed.MOV, maxMOV);		//set moving speed
				int sid = ship.getShipUID();
				
				//check if same ship in other team, cancel the buff in other team
				for(int k = 0; k < 9; k++) {
					//check different team
					if(k != teamID) {
						int[] temp2 = props.checkIsInTeam(sid, k);

						//get ship in other team with formation
						if(temp2[1] > 0) {
							//clear formation buff
							props.setFormatID(k, 0);
//							LogHelper.info("DEBUG : check format "+k+" "+temp2[0]+" "+temp2[1]+" "+ship);
							//set ship update flag
							for(int m = 0; m < 6; m++) {
								ship = props.getShipEntity(k, m);
								
								if(ship != null) {
									ship.setUpdateFlag(ID.FU.FormationBuff, true);  //set update
								}
							}//end for set ship flag
						}//end if ship in other team
					}//end if different team
				}//end for check ship in other team
			}//end if get ship
		}//end for check all ship in team
	}
	
	/** get formation moving speed, SERVER SIDE ONLY */
	public static float getFormationMOV(ExtendPlayerProps props, int teamID) {
		float val = 0F;
		
		if(props != null && teamID >= 0 && teamID < 9) {
			val = props.getMinMOVInTeam(teamID) +
				  getFormationBuffValue(props.getFormatID(teamID), 0)[ID.Formation.MOV];
		}
		
		return val;
	}
	
	/** get buff value by formation/slot id */
	public static float[] getFormationBuffValue(int formationID, int slotID) {
		float[] fvalue = Values.FormationBuffsMap.get(formationID * 10 + slotID);
		
		if(fvalue != null) {
			return fvalue;
		}
		return Values.zeros13;
	}
	
	/** apply ship moving by flag ship position, check is guard BLOCK or ENTITY */
	public static void applyFormationMoving(BasicEntityShip[] ships, int formatID) {
		if(ships != null) {
			//get flag ship
			for(BasicEntityShip s : ships) {
				if(s != null) {
					applyFormationMoving(ships, formatID, (int)s.posX, (int)s.posY, (int)s.posZ);
					break;
				}
			}
		}
	}
	
	/** calc formation guard position
	 * 
	 *  �e�m����:
	 *  ship�����]�w�bStatesMinor�]�wformatPos / formatType
	 *  ����k�ھ�type��pos�]�w���ʦ�m
	 *  
	 *  0. �}��ID
	 *     1: ���a�}, LineAhead
	 *     2: ���a�}, Double Line
	 *     3: ���ΰ}, Diamond
	 *     4: ��ΰ}, Echelon
	 *     5: ���}, Line Abreast
	 * 
	 *  1. �}����V
	 *     �H1 or 2�������, �p�ⶤ���쥻��m��ؼЦ�m��x,z�y�Юt
	 *     x,z�y�Юt���j�����ӧ@���}�����e���V, ex: �Yx���j, �h����e���V�Hx�b����
	 *  
	 *  2. �}����m: 1 or 2���Xĥ
	 *  
	 *     a. ���a�}         1      b. ���a�}                                c. �����}(6��)       (5��, ���H�j�M�춶�Ǭ���)
	 *                2               3  4              2             2
	 *                3               1  2          3   6   4     3       4
	 *                4               5  6              1             1
	 *                5                                 5             5
	 *                6
	 *                
	 *     d. ��ΰ}               1    e. ���}
 	 *                 2
 	 *               3            5 3 1 2 4 6
 	 *             4 
 	 *           5
	 *         6
	 */
	public static void applyFormationMoving(BasicEntityShip[] ships, int formatID, int x, int y, int z) {
		//get flag ship
		int formatPos = 0;
		double dx, dy, dz;
		boolean alongX, faceP;  //along x axis, face positive direction
		BasicEntityShip flagShip = null;
		EntityPlayer owner = null;
		
		//get the toppest ship as flag ship
		for(BasicEntityShip s : ships) {
			if(s != null) {
				flagShip = s;
				owner = EntityHelper.getEntityPlayerByUID(flagShip.getPlayerUID());
				break;
			}
		}
		
		if(flagShip != null && owner != null) {
			//calc moving direction
			dx = (double)x - flagShip.posX;
			dz = (double)z - flagShip.posZ;
			alongX = CalcHelper.isAbsGreater(dx, dz);
			
			if(alongX) {		//along X
				if(dx >= 0) {	//face positive
					faceP = true;
				}
				else {			//face negative
					faceP = false;
				}
			}
			else {				//along Z
				if(dz >= 0) {	//face positive
					faceP = true;
				}
				else {			//face negative
					faceP = false;
				}
			}
			
			/** calc position
			 *  
			 *  1. line ahead:
			 *     applyLineAheadPos�ǤJ�ثe�}����m, �^�ǤU�@���m, ���ݭnformatPos
			 *     �u�ھڶǤJ��m�۰ʦV�}����V����3��
			 *     
			 *  2. other:
			 *     �ǤJflag ship��m��formatPos, �L�^�ǭ�
			 *     �ھ�flag ship��m��formatPos�p��U���m
			 *     
			 */
			int[] newPos = new int[] {x, y, z};
			
			for(BasicEntityShip s : ships) {
				if(s != null) {
					switch(formatID) {
					case 1:  //line ahead
					case 4:  //echelon
						//get next pos
						newPos = setFormationPos1(s, formatID, alongX, faceP, newPos[0], newPos[1], newPos[2]);
						break;
					case 2:  //double line
					case 3:  //diamond
					case 5:  //line abreast
						setFormationPos2(s, formatID, alongX, faceP, newPos[0], newPos[1], newPos[2]);
						break;
					default:
						//apply moving
						EntityHelper.applyShipGuard(s, 1, x, y, z);
						break;
					}
					
					//sync guard
					CommonProxy.channelE.sendTo(new S2CEntitySync(s, 3), (EntityPlayerMP) owner);
				}
			}//end apply to all ships
		}//end get flag ship
	}
	
	/** apply formation position type 1
	 * 
	 *  �A�Ω󪽽u�����}��: line ahead / echelon
	 *  
	 *  input:  prev target pos
	 *  exec:   apply ship guard
	 *  return: next target pos
	 */
	public static int[] setFormationPos1(BasicEntityShip ship, int formatType, boolean alongX, boolean faceP, int x, int y, int z) {
		//get safe pos
		int[] pos = BlockHelper.getSafeBlockWithin5x5(ship.worldObj, x, y, z);
		
		if(pos != null) {
			//apply moving
			EntityHelper.applyShipGuard(ship, 1, pos[0], pos[1], pos[2]);
			LogHelper.info("DEBUG : apply formation move: safe: "+pos[0]+" "+pos[1]+" "+pos[2]);
			
			//return next pos
			switch(formatType) {
			case 4:  //echelon
				return nextEchelonPos(faceP, pos[0], pos[1], pos[2]);
			}
			
			//default formaion = line ahead
			return nextLineAheadPos(alongX, faceP, pos[0], pos[1], pos[2]);
		}
		else {
			//apply moving
			EntityHelper.applyShipGuard(ship, 1, x, y, z);
			LogHelper.info("DEBUG : apply formation move: not safe: "+x+" "+y+" "+z);
			
			return new int[] {x, y, z};
		}
	}
	
	/** apply formation position type 2
	 *  
	 *  �A�Ω�D���u�����}��: double line / diamond / line abreast
	 * 
	 *  input: flag ship pos
	 *  exec:  apply ship guard
	 */
	public static void setFormationPos2(BasicEntityShip ship, int formatType, boolean alongX, boolean faceP, int x, int y, int z) {
		int formatPos = ship.getStateMinor(ID.M.FormatPos);
		int[] pos = new int[] {x, y, z};
		
		//check error position
		if(formatPos < 0 || formatPos > 5) formatPos = 0;

		//calc next pos
		switch(formatType) {
		case 2:  //double line
			pos = nextDoubleLinePos(alongX, faceP, formatPos, pos[0], pos[1], pos[2]);
			break;
		case 3:  //diamond
			pos = nextDiamondPos(alongX, faceP, formatPos, pos[0], pos[1], pos[2]);
			break;
		case 5:  //line abreast
			pos = nextLineAbreastPos(alongX, formatPos, pos[0], pos[1], pos[2]);
			break;
		}
		
		//get safe pos
		pos = BlockHelper.getSafeBlockWithin5x5(ship.worldObj, pos[0], pos[1], pos[2]);
		
		if(pos != null) {
			//apply moving
			EntityHelper.applyShipGuard(ship, 1, pos[0], pos[1], pos[2]);
			LogHelper.info("DEBUG : apply formation move: safe: "+pos[0]+" "+pos[1]+" "+pos[2]);
		}
		else {
			//apply moving
			EntityHelper.applyShipGuard(ship, 1, x, y, z);
			LogHelper.info("DEBUG : apply formation move: not safe: "+x+" "+y+" "+z);
		}
	}
	
	/** calc next LINE AHEAD pos
	 *  ���a�}         0
	 *          1
	 *          2
	 *          3
	 *          4
	 *          5
	 */
	public static int[] nextLineAheadPos(boolean alongX, boolean faceP, int x, int y, int z) {
		int[] pos = new int[] {x, y, z};
		
		//calc next pos
		if(alongX) {		//along X
			if(faceP) {		//face positive
				pos[0] = pos[0] - 3;
			}
			else {			//face negative
				pos[0] = pos[0] + 3;
			}
		}
		else {				//along Z
			if(faceP) {		//face positive
				pos[2] = pos[2] - 3;
			}
			else {			//face negative
				pos[2] = pos[2] + 3;
			}
		}
		
		return pos;
	}
	
	/** calc next DOUBLE LINE pos
	 *  ���a�}      2  3
	 *         0  1
	 *         4  5
	 */
	public static int[] nextDoubleLinePos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
		int[] pos = new int[] {x, y, z};
		
		//calc target block by formatPos
		switch(formatPos) {
		case 1:
			if(alongX) {		//along X
				pos[2] = pos[2] + 3;
			}
			else {				//along Z
				pos[0] = pos[0] + 3;
			}
			break;
		case 2:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 3;
				}
				else {			//face negative
					pos[0] = pos[0] - 3;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[2] = pos[2] + 3;
				}
				else {			//face negative
					pos[2] = pos[2] - 3;
				}
			}
			break;
		case 3:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] + 3;
				}
				else {			//face negative
					pos[0] = pos[0] - 3;
					pos[2] = pos[2] + 3;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] + 3;
				}
				else {			//face negative
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] - 3;
				}
			}
			break;
		case 4:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] - 3;
				}
				else {			//face negative
					pos[0] = pos[0] + 3;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[2] = pos[2] - 3;
				}
				else {			//face negative
					pos[2] = pos[2] + 3;
				}
			}
			break;
		case 5:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] - 3;
					pos[2] = pos[2] + 3;
				}
				else {			//face negative
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] + 3;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] - 3;
				}
				else {			//face negative
					pos[0] = pos[0] + 3;
					pos[2] = pos[2] + 3;
				}
			}
			break;
		}
		
		return pos;
	}
	
	/** calc next DIAMOND pos
	 *  �����}              1
	 *         2  5  3
	 *            0
	 *            4
	 */
	public static int[] nextDiamondPos(boolean alongX, boolean faceP, int formatPos, int x, int y, int z) {
		int[] pos = new int[] {x, y, z};
		
		//calc target block by formatPos
		switch(formatPos) {
		case 1:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 5;
				}
				else {			//face negative
					pos[0] = pos[0] - 5;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[2] = pos[2] + 5;
				}
				else {			//face negative
					pos[2] = pos[2] - 5;
				}
			}
			break;
		case 2:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 1;
					pos[2] = pos[2] - 4;
				}
				else {			//face negative
					pos[0] = pos[0] - 1;
					pos[2] = pos[2] - 4;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[0] = pos[0] - 4;
					pos[2] = pos[2] + 1;
				}
				else {			//face negative
					pos[0] = pos[0] - 4;
					pos[2] = pos[2] - 1;
				}
			}
			break;
		case 3:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 1;
					pos[2] = pos[2] + 4;
				}
				else {			//face negative
					pos[0] = pos[0] - 1;
					pos[2] = pos[2] + 4;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[0] = pos[0] + 4;
					pos[2] = pos[2] + 1;
				}
				else {			//face negative
					pos[0] = pos[0] + 4;
					pos[2] = pos[2] - 1;
				}
			}
			break;
		case 4:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] - 3;
				}
				else {			//face negative
					pos[0] = pos[0] + 3;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[2] = pos[2] - 3;
				}
				else {			//face negative
					pos[2] = pos[2] + 3;
				}
			}
			break;
		case 5:
			if(alongX) {		//along X
				if(faceP) {		//face positive
					pos[0] = pos[0] + 2;
				}
				else {			//face negative
					pos[0] = pos[0] - 2;
				}
			}
			else {				//along Z
				if(faceP) {		//face positive
					pos[2] = pos[2] + 2;
				}
				else {			//face negative
					pos[2] = pos[2] - 2;
				}
			}
			break;
		}
		
		return pos;
	}
	
	/** calc next ECHELON pos
	 *  ��ΰ}                       0
	 *              1
	 *            2
	 *          3
	 *        4
	 *      5
	 */
	public static int[] nextEchelonPos(boolean faceP, int x, int y, int z) {
		int[] pos = new int[] {x, y, z};
		
		//calc next pos
		if(faceP) {		//face positive
			pos[0] = pos[0] - 2;
			pos[2] = pos[2] - 2;
		}
		else {			//face negative
			pos[0] = pos[0] + 2;
			pos[2] = pos[2] + 2;
		}
		
		return pos;
	}
	
	/** calc next LINE ABREAST pos
	 *  ���}
	 *  
	 *  4  2  0  1  3  5
	 *  
	 */
	public static int[] nextLineAbreastPos(boolean alongX, int formatPos, int x, int y, int z) {
		int[] pos = new int[] {x, y, z};
		
		//calc target block by formatPos
		switch(formatPos) {
		case 1:
			if(alongX) {		//along X
				pos[2] = pos[2] + 3;
			}
			else {				//along Z
				pos[0] = pos[0] + 3;
			}
			break;
		case 2:
			if(alongX) {		//along X
				pos[2] = pos[2] - 3;
			}
			else {				//along Z
				pos[0] = pos[0] - 3;
			}
			break;
		case 3:
			if(alongX) {		//along X
				pos[2] = pos[2] + 6;
			}
			else {				//along Z
				pos[0] = pos[0] + 6;
			}
			break;
		case 4:
			if(alongX) {		//along X
				pos[2] = pos[2] - 6;
			}
			else {				//along Z
				pos[0] = pos[0] - 6;
			}
			break;
		case 5:
			if(alongX) {		//along X
				pos[2] = pos[2] + 9;
			}
			else {				//along Z
				pos[0] = pos[0] + 9;
			}
			break;
		}
		
		return pos;
	}
	
	
	
}
