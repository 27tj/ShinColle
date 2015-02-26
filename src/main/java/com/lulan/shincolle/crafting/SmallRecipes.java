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
 * Equip Build Rate:
 *   Small Shipyard: single & weak twin cannon (equipID 0~4)
 *     grudge   -> +1.5p
 *     abyssium -> +4.0p
 *     ammo     -> +1.0p
 *     polymetal-> +2.0p
 *     
 *     min point (all 16) = 136
 *       result: AmmoCon 25% AmmoHCon 25% eq0 30% eq1 15% eq2 5%  eq3 0%  eq4 0%
 *     max point (all 64) = 544
 *       result: AmmoCon 0%  AmmoHCon 0%  eq0 20% eq1 25% eq2 20% eq3 20% eq4 15%
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
		//�p������`��: grudge 1.5p abyss 4p ammo 1p poly 2p
		float[] equipChance = new float[7];
		float pointMod = (matAmount[0]*1.5F + matAmount[1]*4F + matAmount[2]*1F + matAmount[3]*2F - 136F) / 408F;
		
		//equipChance: 0:AmmoContainer 1:AmmoHeavyContainer 2:eq0 3:eq1 4:eq2 5:eq3 6:eq4
		//�����ֿn���v(cumulate chance)
		equipChance[0] = 0.25F - 0.25F * pointMod;
		equipChance[1] = 0.25F - 0.25F * pointMod + equipChance[0];
		equipChance[2] = 0.3F - 0.1F * pointMod + equipChance[1];
		equipChance[3] = 0.15F + 0.1F * pointMod + equipChance[2];
		equipChance[4] = 0.05F + 0.15F * pointMod + equipChance[3];
		equipChance[5] = 0F + 0.2F * pointMod + equipChance[4];
		equipChance[6] = 0F + 0.15F * pointMod + equipChance[5];
		
		LogHelper.info("DEBUG : roll equip chance: "+String.format("%.2f", equipChance[0])+" "+
						String.format("%.2f", equipChance[1])+" "+String.format("%.2f", equipChance[2])+" "+
						String.format("%.2f", equipChance[3])+" "+String.format("%.2f", equipChance[4])+" "+
						String.format("%.2f", equipChance[5])+" "+String.format("%.2f", equipChance[6]));
		
		//roll
		float roll = rand.nextFloat();
		int rollResult = 0;
		//�qarray�̫᩹�e��Ӷ}�l��, �Y�몺��i���~�����v��, ��ܥi�H�����i+1�Ӫ��~
		for(int i = (equipChance.length - 2); i >= 0; i--) {
			if(roll > equipChance[i]) {
				rollResult = i + 1;
				break;
			}
		}
		
		//get result item
		ItemStack buildResult = null;
		switch(rollResult) {
		case 0:
			buildResult = new ItemStack(ModItems.Ammo, 11+rand.nextInt(11), 1);
			break;
		case 1:
			buildResult = new ItemStack(ModItems.Ammo, 2+rand.nextInt(2), 3);
			break;
		case 2:
			buildResult = new ItemStack(ModItems.EquipCannon, 1, 0);
			break;
		case 3:
			buildResult = new ItemStack(ModItems.EquipCannon, 1, 1);
			break;
		case 4:
			buildResult = new ItemStack(ModItems.EquipCannon, 1, 2);
			break;
		case 5:
			buildResult = new ItemStack(ModItems.EquipCannon, 1, 3);
			break;
		case 6:
			buildResult = new ItemStack(ModItems.EquipCannon, 1, 4);
			break;
		}
		
		LogHelper.info("DEBUG : roll result: "+roll+" "+buildResult);
		return buildResult;
	}
	

}
