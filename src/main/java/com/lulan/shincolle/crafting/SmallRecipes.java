package com.lulan.shincolle.crafting;

import com.lulan.shincolle.init.ModItems;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;

/** Small Shipyard Recipe Helper
 *  Fuel Cost = BaseCost + CostPerMaterial * ( TotalMaterialAmount - minAmount * 4 )
 *  Total Build Time = FuelCost / buildSpeed
 *  MaxBuildTime / MaxFuelCost = 8min / 460800  (48 fuel per tick)
 *  MinBuildTime / MinFuelCost = 1min / 57600
 * 	MaxMaterial / MaxFuelCost = 64*4 / 460800
 *  MinMaterial / MinFuelCost = 16*4 / 57600 = BaseCost(57600) CostPerMaterial(2100)
 */	
public class SmallRecipes {
	
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
			return (item == ModItems.Grudge)||(item == ModItems.Abyssium)||(item == ModItems.Ammo)||(item == ModItems.Polymetal);
		}
		return false;
	}
	
	//�P�w���ƺ���: 0:grudge 1:abyss 2:ammo 3:poly 4:fuel -1:other
	public static byte getMaterialID(ItemStack itemstack) {
		Item item = itemstack.getItem();
		byte itemID = -1;
		
		if(item == ModItems.Grudge) itemID = 0;
		if(item == ModItems.Abyssium) itemID = 1;
		if(item == ModItems.Ammo) itemID = 2;
		if(item == ModItems.Polymetal) itemID = 3;
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
	public static ItemStack getBuildResult(byte[] matAmount) {
		//�ˬd�|�ӧ��Ƽƶq�O�_���W�L�̤p��
		ItemStack buildResult = new ItemStack(ModItems.ShipSpawnEgg);
		buildResult.setItemDamage(0);
		buildResult.stackTagCompound = new NBTTagCompound();
		buildResult.stackTagCompound.setByte("Grudge", matAmount[0]);
		buildResult.stackTagCompound.setByte("Abyssium", matAmount[1]);
		buildResult.stackTagCompound.setByte("Ammo", matAmount[2]);
		buildResult.stackTagCompound.setByte("Polymetal", matAmount[3]);
		
		return buildResult;
	}
	
}
