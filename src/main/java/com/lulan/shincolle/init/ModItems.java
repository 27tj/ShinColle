package com.lulan.shincolle.init;

import net.minecraft.item.Item;

import com.lulan.shincolle.item.Abyssium;
import com.lulan.shincolle.item.Ammo;
import com.lulan.shincolle.item.BasicItem;
import com.lulan.shincolle.item.BasicEquip;
import com.lulan.shincolle.item.BucketRepair;
import com.lulan.shincolle.item.Grudge;
import com.lulan.shincolle.item.HeavyAmmo;
import com.lulan.shincolle.item.Polymetal;
import com.lulan.shincolle.item.ShipSpawnEgg;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(Reference.MOD_ID)	//�n��object holder��mod������e���y�q ��L�H�i�H����Ū���Ӫ���
public class ModItems {

	public static final BasicItem Abyssium = new Abyssium();
	public static final BasicItem Ammo = new Ammo();
	public static final BasicItem BucketRepair = new BucketRepair();
	public static final BasicItem Grudge = new Grudge();
	public static final BasicItem HeavyAmmo = new HeavyAmmo();
	public static final BasicItem Polymetal = new Polymetal();
	public static final ShipSpawnEgg ShipSpawnEgg = new ShipSpawnEgg();
	
	public static final BasicItem BasicEquip = new BasicEquip();	//debug
	

	//�n��item��C���� (�bpre init���q�n��)
	public static void init() {
		//items
		GameRegistry.registerItem(Abyssium, "Abyssium");
		GameRegistry.registerItem(Ammo, "Ammo");
		GameRegistry.registerItem(BucketRepair, "BucketRepair");
		GameRegistry.registerItem(Grudge, "Grudge");
		GameRegistry.registerItem(HeavyAmmo, "HeavyAmmo");
		GameRegistry.registerItem(Polymetal, "Polymetal");
		GameRegistry.registerItem(ShipSpawnEgg, "ShipSpawnEgg");
		
		GameRegistry.registerItem(BasicEquip, "BasicEquip");
		
	}
	
}
