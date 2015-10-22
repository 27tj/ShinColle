package com.lulan.shincolle.init;

import com.lulan.shincolle.item.AbyssMetal;
import com.lulan.shincolle.item.Ammo;
import com.lulan.shincolle.item.BasicItem;
import com.lulan.shincolle.item.EquipAirplane;
import com.lulan.shincolle.item.EquipArmor;
import com.lulan.shincolle.item.EquipCannon;
import com.lulan.shincolle.item.EquipCatapult;
import com.lulan.shincolle.item.EquipRadar;
import com.lulan.shincolle.item.EquipTorpedo;
import com.lulan.shincolle.item.EquipTurbine;
import com.lulan.shincolle.item.Grudge;
import com.lulan.shincolle.item.InstantConMat;
import com.lulan.shincolle.item.KaitaiHammer;
import com.lulan.shincolle.item.MarriageRing;
import com.lulan.shincolle.item.ModernKit;
import com.lulan.shincolle.item.OwnerPaper;
import com.lulan.shincolle.item.PointerItem;
import com.lulan.shincolle.item.RepairBucket;
import com.lulan.shincolle.item.RepairGoddess;
import com.lulan.shincolle.item.ShipSpawnEgg;
import com.lulan.shincolle.item.ToyAirplane;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.GameRegistry;

@GameRegistry.ObjectHolder(Reference.MOD_ID)	//�n��object holder��mod������e���y�q ��L�H�i�H����Ū���Ӫ���
public class ModItems {

	//spawn egg
	public static final ShipSpawnEgg ShipSpawnEgg = new ShipSpawnEgg();
	//materials
	public static final BasicItem AbyssMetal = new AbyssMetal();
	public static final BasicItem Ammo = new Ammo();
	public static final BasicItem Grudge = new Grudge();
	//equip	
	public static final BasicItem EquipAirplane = new EquipAirplane();
	public static final BasicItem EquipArmor = new EquipArmor();
	public static final BasicItem EquipCannon = new EquipCannon();
	public static final BasicItem EquipCatapult = new EquipCatapult();
	public static final BasicItem EquipRadar = new EquipRadar();
	public static final BasicItem EquipTorpedo = new EquipTorpedo();
	public static final BasicItem EquipTurbine = new EquipTurbine();
	//misc
	public static final BasicItem BucketRepair = new RepairBucket();
	public static final BasicItem InstantConMat = new InstantConMat();
	public static final BasicItem KaitaiHammer = new KaitaiHammer();
	public static final BasicItem MarriageRing = new MarriageRing();
	public static final BasicItem ModernKit = new ModernKit();
	public static final BasicItem OwnerPaper = new OwnerPaper();
	public static final BasicItem PointerItem = new PointerItem();
	public static final BasicItem RepairGoddess = new RepairGoddess();
	//toy
	public static final BasicItem ToyAirplane = new ToyAirplane();

	//�n��item��C���� (�bpre init���q�n��)
	public static void init() {
		//spawn egg
		GameRegistry.registerItem(ShipSpawnEgg, "ShipSpawnEgg");
		//materials
		GameRegistry.registerItem(AbyssMetal, "AbyssMetal");
		GameRegistry.registerItem(Ammo, "Ammo");
		GameRegistry.registerItem(Grudge, "Grudge");
		//equip		
		GameRegistry.registerItem(EquipAirplane, "EquipAirplane");
		GameRegistry.registerItem(EquipArmor, "EquipArmor");
		GameRegistry.registerItem(EquipCannon, "EquipCannon");
		GameRegistry.registerItem(EquipCatapult, "EquipCatapult");
		GameRegistry.registerItem(EquipRadar, "EquipRadar");
		GameRegistry.registerItem(EquipTorpedo, "EquipTorpedo");
		GameRegistry.registerItem(EquipTurbine, "EquipTurbine");
		//misc
		GameRegistry.registerItem(BucketRepair, "BucketRepair");
		GameRegistry.registerItem(InstantConMat, "InstantConMat");
		GameRegistry.registerItem(KaitaiHammer, "KaitaiHammer");
		GameRegistry.registerItem(MarriageRing, "MarriageRing");
		GameRegistry.registerItem(ModernKit, "ModernKit");
		GameRegistry.registerItem(OwnerPaper, "OwnerPaper");
		GameRegistry.registerItem(PointerItem, "PointerItem");
		GameRegistry.registerItem(RepairGoddess, "RepairGoddess");
		//toy
		GameRegistry.registerItem(ToyAirplane, "ToyAirplane");
	}
	
}
