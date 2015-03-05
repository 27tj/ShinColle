package com.lulan.shincolle.crafting;

import java.util.Random;

import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;

/**Small Shipyard Recipe Helper
 *  Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 8min / 460800  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 1min / 57600
 * 	MaxMaterial / MaxFuelCost = 64*4 / 460800
 *  MinMaterial / MinFuelCost = 16*4 / 57600 = BaseCost(57600) CostPerMaterial(2100)
 *  
 *  
 * Equip Build Rate: first roll -> second roll -> third roll
 * 	 1. FIRST: roll ammo or equip
 *		ammo rate: total 64 (16x4) = 50%, total 128 = 0%
 *		equip rate: 1 - ammo
 *
 * 	 2. SECOND: if equip, roll equip type
 *              if ammo, roll ammo type and quantity
 *              
 * 	 3. THIRD: roll equips of the type
 */	
public class SmallRecipes {
	
	private static Random rand = new Random();
	private static final byte minAmount = 16;		//material min amount
	private static final int basePower = 57600;		//base cost power
	private static final int powerPerMat = 2100;	//cost per item
	
	public SmallRecipes() {}
		
	//�ˬd���ƬO�_����سy
	public static boolean canRecipeBuild(byte[] matAmount) {
		return matAmount[0]>=minAmount && matAmount[1]>=minAmount && matAmount[2]>=minAmount && matAmount[3]>=minAmount;
	}
	
	//�p���`�@�ݭn���U��
	public static int calcGoalPower(byte[] matAmount) {
		int extraAmount;
		
		if(canRecipeBuild(matAmount)) {
			extraAmount = (int) matAmount[0] + (int) matAmount[1] + (int) matAmount[2] + (int) matAmount[3] - (int)(minAmount) * 4;
			return basePower + powerPerMat * extraAmount;
		}
		
		return 0;
	}
	
	//�P�w���~�O�_������
	public static boolean isMaterial(ItemStack itemstack) {
		if(itemstack != null) {
			Item item = itemstack.getItem();
			int meta = itemstack.getItemDamage();
			return (item == ModItems.Grudge)||
				   (item == ModItems.AbyssMetal && meta == 0)||
				   (item == ModItems.Ammo && meta == 0)||
				   (item == ModItems.AbyssMetal && meta == 1);
		}
		return false;
	}
	
	//�P�w���ƺ���: 0:grudge 1:abyss 2:ammo 3:poly 4:fuel -1:other
	public static byte getMaterialType(ItemStack itemstack) {
		Item item = itemstack.getItem();
		int meta = itemstack.getItemDamage();
		byte itemID = -1;
		
		if(item == ModItems.Grudge) itemID = 0;
		if(item == ModItems.AbyssMetal && meta == 0) itemID = 1;
		if(item == ModItems.Ammo && meta == 0) itemID = 2;
		if(item == ModItems.AbyssMetal && meta == 1) itemID = 3;
		if(TileEntityFurnace.isItemFuel(itemstack))  itemID = 4;
		
		return itemID;
	}
	
	//���o�|�˧��ƭӼ�with null check
	//itemstack:0:grudge 1:abyss 2:ammo 3:poly 4:fuel 5:output
	public static byte[] getMaterialAmount(ItemStack[] item) {
		byte[] itemAmount = new byte[4];
		
		for(int i=0; i<4; i++) {	//���oitem 0~3�����, �Y�|�˧��Ƹ��
			if(item[i] != null) {	//�[�Wnull�P�_�H�KNPE
				itemAmount[i] = (byte) item[i].stackSize;
			}
			else {
				itemAmount[i] = 0;
			}
		}
		
		return itemAmount;		
	}
	
	//�N���Ƽƶq�g�iitemstack�^��
	public static ItemStack getBuildResultShip(byte[] matAmount) {
		ItemStack buildResult = new ItemStack(ModItems.ShipSpawnEgg);
		buildResult.setItemDamage(0);
		buildResult.stackTagCompound = new NBTTagCompound();
		buildResult.stackTagCompound.setByte("Grudge", matAmount[0]);
		buildResult.stackTagCompound.setByte("Abyssium", matAmount[1]);
		buildResult.stackTagCompound.setByte("Ammo", matAmount[2]);
		buildResult.stackTagCompound.setByte("Polymetal", matAmount[3]);
		
		return buildResult;
	}
	
	//�N���Ƽƶq�g�iitemstack�^��
	public static ItemStack getBuildResultEquip(byte[] matAmount) {	
		//result item
		ItemStack buildResult = null;
		int totalMats = matAmount[0] + matAmount[1] + matAmount[2] + matAmount[3];
		int[] matsInt = new int[] {0,0,0,0};
		int rollType = -1;
		float equipRate = totalMats / 128F;
		float randRate = rand.nextFloat();
		
		if(equipRate > 1F) equipRate = 1F;	//min 50%, max 100%	
		LogHelper.info("DEBUG : equip build roll: rate / random "+String.format("%.2f", equipRate)+" "+String.format("%.2f", randRate));	
		//first roll: roll equip or ammo
		if(randRate < equipRate) {	//get equip 
			//second roll: roll equip type
			matsInt[0] = matAmount[0];
			matsInt[1] = matAmount[1];
			matsInt[2] = matAmount[2];
			matsInt[3] = matAmount[3];
			rollType = EquipCalc.rollEquipType(0, matsInt);
			//third roll: roll equips of the type
			return EquipCalc.rollEquipsOfTheType(rollType);
			
		}
		else {								//get ammo
			//second roll: roll ammo type and quantity
			//50% for light or heavy ammo container
			if(rand.nextInt(2) == 0) {	
				buildResult = new ItemStack(ModItems.Ammo, 11+rand.nextInt(11), 1);
			}
			else {
				buildResult = new ItemStack(ModItems.Ammo, 2+rand.nextInt(2), 3);
			}
			return buildResult;	
		}
	}
	

}
