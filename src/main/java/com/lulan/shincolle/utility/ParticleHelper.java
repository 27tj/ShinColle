package com.lulan.shincolle.utility;

import net.minecraft.world.World;

/**�ɤl�S�ĳB�zclass
 * �]�t�I�s�S��, ����S�Ħ�m(NxNxN����), 
 */
public class ParticleHelper {
	
	/**in:��l�y��, ���, �H�έn�઺���V 	out:�৹���s��m
	 * �{���q�S�����W�U½��, �ҥHy�Ȥ��|�ܰ�
	 * f = face = 0,4:north  1,5:east  2,6:south  3,7:west
	 */
	public static double[] getNewPosition(double x, double y, double z, int f, int len) {
		
		double[] newParm = new double[3];
		newParm[1] = y;
		
		//�̷ӭ��V, �����l��m
		switch(f) {
		case 1:		//turn east
		case 5:
			newParm[0] = (double)len - z;
			newParm[2] = x;
			break;
		case 2:		//turn south
		case 6:
			newParm[0] = (double)len - x;
			newParm[2] = (double)len - z;
			break;
		case 3:		//turn west
		case 7:
			newParm[0] = z;
			newParm[2] = (double)len - x;
			break;
		default:	//default north, no change
			newParm[0] = x;
			newParm[2] = z;
			break;			
		}
			
		return newParm;
	}

}
