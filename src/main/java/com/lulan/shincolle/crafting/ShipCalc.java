package com.lulan.shincolle.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**SHIP SPAWN CALC <br>
 * Material Bonus Rate:
 *		Grudge    -> HP ATK DEF SPD MOV HIT	<br>
 *      Abyssium  -> HP DEF					<br>
 *      Ammo      -> ATK SPD				<br>
 * 		Polymetal -> MOV HIT				<br><br>
 * 
 * base rate: {64,16,4,1}  					<br>
 * +3 rate: base + amount * 0.25			<br>
 * +2 rate: base + amount * 0.375			<br>
 * +1 rate: base + amount * 0.75			<br>
 * +0 rate: base + amount * 0.25			<br>
 * 
 * min consume(all 16) = {72,40,16,9}   = {52.6%, 29.2%, 11.7%, 6.6%}
 * max consume(all 64) = {96,112,52,33} = {32.8%, 38.2%, 17.7%, 11.3%}
 * 
 * 
 * Ship Build Rate: first roll -> second roll
 * 	 1. FIRST: roll ship type
 * 
 * 	 2. SECOND: roll ships of the type
 */
public class ShipCalc {
	
	private static Random rand = new Random();
	private static final float[] baseRate = new float[] {64F, 16F, 4F, 1F};
	
	//in: itemstack(with materials amount)		out: ATK HP DEF SPD MOV HIT bonus value
	public static byte[] getBonusPoints(ItemStack itemstack) {
		byte[] bonusPoint = new byte[6];		//HP ATK DEF SPD MOV HIT
		byte[] matAmount = getMatAmount(itemstack);	//0:grudge 1:abyssium 2:ammo 3:polymetal
				
		float[] rateHP = calcBonusRate((float)(matAmount[0]+matAmount[1]));	  //HP = grudge + abyssium
		float[] rateATK = calcBonusRate((float)(matAmount[0]+matAmount[2]));  //ATK = grudge + ammo
		float[] rateDEF = calcBonusRate((float)(matAmount[0]+matAmount[1]));  //DEF = grudge + abyssium
		float[] rateSPD = calcBonusRate((float)(matAmount[0]+matAmount[2]));  //SPD = grudge + ammo
		float[] rateMOV = calcBonusRate((float)(matAmount[0]+matAmount[3]));  //MOV = grudge + polymetal
		float[] rateHIT = calcBonusRate((float)(matAmount[0]+matAmount[3]));  //HIT = grudge + polymetal

		bonusPoint[0] = rollBonusValue(rateHP);
		bonusPoint[1] = rollBonusValue(rateATK);
		bonusPoint[2] = rollBonusValue(rateDEF);
		bonusPoint[3] = rollBonusValue(rateSPD);
		bonusPoint[4] = rollBonusValue(rateMOV);
		bonusPoint[5] = rollBonusValue(rateHIT);
	
		return bonusPoint;
	}

	//get material amount from nbt data
	private static byte[] getMatAmount(ItemStack itemstack) {
		byte[] matAmount = {0,0,0,0};	//grudge, abyssium, ammo, polymetal
				
		if(itemstack.hasTagCompound()) {		
			matAmount[0] = itemstack.getTagCompound().getByte("Grudge");
			matAmount[1] = itemstack.getTagCompound().getByte("Abyssium");
			matAmount[2] = itemstack.getTagCompound().getByte("Ammo");
			matAmount[3] = itemstack.getTagCompound().getByte("Polymetal");
		}
	
		LogHelper.info("DEBUG : shipcalc get matAmount : "+matAmount[0]+" "+matAmount[1]+" "+matAmount[2]+" "+matAmount[3]);
		return matAmount;
	}

	//in: material amount		out: +0~+3 rate
	private static float[] calcBonusRate(float i) {
		float[] newRate = new float[4];
		float total = 1F;	//avoid divide by zero
		
		newRate[0] = baseRate[0] + i * 0.25F;		//+0
		newRate[1] = baseRate[1] + i * 0.75F;		//+1
		newRate[2] = baseRate[2] + i * 0.375F;		//+2
		newRate[3] = baseRate[3] + i * 0.25F;		//+3
		
		total = newRate[0]+newRate[1]+newRate[2]+newRate[3];
		
		newRate[0] /= total;
		newRate[1] /= total;
		newRate[2] /= total;
		newRate[3] /= total;	
		
		LogHelper.info("DEBUG : shipcalc baseRate : "+baseRate[0]+" "+baseRate[1]+" "+baseRate[2]+" "+baseRate[3]);
		LogHelper.info("DEBUG : shipcalc newRate : "+newRate[0]+" "+newRate[1]+" "+newRate[2]+" "+newRate[3]);
		return newRate;
	}
	
	//roll +0~+3 according to rate
	private static byte rollBonusValue(float[] rate) {
		float ranNum = (float)(rand.nextInt(100) + 1)/100F;		//random 0.01~1.00
		float bonus3 = rate[0]+rate[1]+rate[2];
		float bonus2 = rate[0]+rate[1];
		float bonus1 = rate[0];
		
		LogHelper.info("DEBUG : bonus random : "+ranNum+" +3: "+bonus3+" +2: "+bonus2+" +1: "+bonus1);
		
		if(ranNum > bonus3) {
			LogHelper.info("DEBUG : get bonus point : 3");
			return 3;
		}
		else if(ranNum > bonus2) {
			LogHelper.info("DEBUG : get bonus point : 2");
			return 2;
		}
		else if(ranNum > bonus1) {
			LogHelper.info("DEBUG : get bonus point : 1");
			return 1;
		}
		else {
			LogHelper.info("DEBUG : get bonus point : 0");
			return 0;
		}
	}
	
	//roll ship type by total number of materials
	public static int rollShipType(ItemStack item) {
		List<int[]> shipList = new ArrayList<int[]>();
		int[] material = new int[4];
		int totalMats = 0;
		
		if(item.getItemDamage() > 1) {	//is debug egg
			return item.getItemDamage() - 2;
		}
		
		//get materials amount
    	if (item.stackTagCompound != null) { 	//正常製造egg, 會有四個材料tag		
    		material[0] = item.stackTagCompound.getInteger("Grudge");
    		material[1] = item.stackTagCompound.getInteger("Abyssium");
    		material[2] = item.stackTagCompound.getInteger("Ammo");
    		material[3] = item.stackTagCompound.getInteger("Polymetal");
    		
    		totalMats = material[0] + material[1] + material[2] + material[3];
        }
    	
    	if(item.getItemDamage() == 0) {	//small egg
    		//Destroyer/Transport/Submarine
    		shipList.add(new int[] {ID.S_DestroyerI, 150});
    		
    		if(totalMats > 128) {	//>128 : LCruiser/HCruiser
    			shipList.add(new int[] {ID.S_HeavyCruiserRI, 50 + material[2] / 2});
    		}

    	}
    	else {							//large egg
    		shipList.add(new int[] {ID.S_CarrierWO, 300 + material[3] / 4});
    		
    		if(totalMats > 1600) {
    			shipList.add(new int[] {ID.S_BattleshipRE, 100 + material[2] / 4});
    		}
    	}

    	//roll item
		int totalRate = 0;
		int sumRate = 0;
		int randNum = 0;
		int rollResult = -1;
		
		//get sum of rate
		for(int[] iter : shipList) {	
			totalRate += iter[1];
		}
		
		//get random number
		randNum = rand.nextInt(totalRate);
		
		//get result
		for(int i = 0; i < shipList.size(); ++i) {
			sumRate += shipList.get(i)[1];
			
			if(sumRate > randNum) {
				rollResult = shipList.get(i)[0];
				break;
			}	
		}
		LogHelper.info("DEBUG : roll ship type: total "+totalRate+" rand "+randNum+" type "+rollResult);	
		return rollResult;
	}
	
	/** Get entity name from metadata
  	 *  (entity name from ModEntity.class)
  	 *  small egg: all non-hime ship
  	 *  large egg: all ship
  	 *  specific egg: specific ship
  	 */
  	public static String getEntityToSpawnName(int type) {
  		switch(type) {
  		case ID.S_DestroyerI:
  			return "shincolle.EntityDestroyerI";
  		case ID.S_HeavyCruiserRI:
  			return "shincolle.EntityHeavyCruiserRi";
  		case ID.S_CarrierWO:
  			return "shincolle.EntityCarrierWo";
  		case ID.S_BattleshipRE:
  			return "shincolle.EntityBattleshipRe";
  		default:
  			return "shincolle.EntityDestroyerI";
  		}	
  	}
  	
	
}
