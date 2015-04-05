package com.lulan.shincolle.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.entity.*;
import com.lulan.shincolle.entity.renderentity.*;
import com.lulan.shincolle.item.BasicEntityItem;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

/**
// register natural spawns for entities
// EntityRegistry.addSpawn(MyEntity.class, spawnProbability, minSpawn, maxSpawn, enumCreatureType, [spawnBiome]);
// See the constructor in BiomeGenBase.java to see the rarity of vanilla mobs; Sheep are probability 10 while Endermen are probability 1
// minSpawn and maxSpawn are about how groups of the entity spawn
// enumCreatureType represents the "rules" Minecraft uses to determine spawning, based on creature type. By default, you have three choices:
//    EnumCreatureType.creature uses rules for animals: spawn everywhere it is light out.
//    EnumCreatureType.monster uses rules for monsters: spawn everywhere it is dark out.
//    EnumCreatureType.waterCreature uses rules for water creatures: spawn only in water.
// [spawnBiome] is an optional parameter of type BiomeGenBase that limits the creature spawn to a single biome type. Without this parameter, it will spawn everywhere. 
// For the biome type you can use an list, but unfortunately the built-in biomeList contains
// null entries and will crash, so you need to clean up that list.
// Diesieben07 suggested the following code to remove the nulls and create list of all biomes:
// BiomeGenBase[] allBiomes = Iterators.toArray(Iterators.filter(Iterators.forArray(BiomeGenBase.getBiomeGenArray()), Predicates.notNull()), BiomeGenBase.class);
// example
// EntityRegistry.addSpawn(EntityLion.class, 6, 1, 5, EnumCreatureType.creature, BiomeGenBase.savanna); //change the values to vary the spawn rarity, biome, etc.              
// EntityRegistry.addSpawn(EntityElephant.class, 10, 1, 5, EnumCreatureType.creature, BiomeGenBase.savanna); //change the values to vary the spawn rarity, biome, etc.              
*/
public class ModEntity {
	
	private static int modEntityID = 2000;

	public static void init() {
		//register test entity
//		createEntityGlobalID(EntityTest.class, "EntityTest", 0x20FF45, 0x0040FF);
		
		//register ship entity
		createEntity(EntityBattleshipNGT.class, "EntityBattleshipNGT", modEntityID++);
		createEntity(EntityBattleshipNGTBoss.class, "EntityBattleshipNGTBoss", modEntityID++);
		createEntity(EntityBattleshipRe.class, "EntityBattleshipRe", modEntityID++);
		createEntity(EntityBattleshipTa.class, "EntityBattleshipTa", modEntityID++);
		createEntity(EntityCarrierWo.class, "EntityCarrierWo", modEntityID++);
		createEntity(EntityDestroyerI.class, "EntityDestroyerI", modEntityID++);
		createEntity(EntityDestroyerRo.class, "EntityDestroyerRo", modEntityID++);
		createEntity(EntityDestroyerHa.class, "EntityDestroyerHa", modEntityID++);
		createEntity(EntityDestroyerNi.class, "EntityDestroyerNi", modEntityID++);
		createEntity(EntityDestroyerShimakaze.class, "EntityDestroyerShimakaze", modEntityID++);
		createEntity(EntityDestroyerShimakazeBoss.class, "EntityDestroyerShimakazeBoss", modEntityID++);
		createEntity(EntityHeavyCruiserRi.class, "EntityHeavyCruiserRi", modEntityID++);
		createEntity(EntityRensouhou.class, "EntityRensouhou", modEntityID++);
		createEntity(EntityRensouhouBoss.class, "EntityRensouhouBoss", modEntityID++);
		createEntity(EntityRensouhouS.class, "EntityRensouhouS", modEntityID++);
		
		//register projectile entity
		createProjectileEntity(EntityAbyssMissile.class, "EntityAbyssMissile", modEntityID++);
		createProjectileEntity(EntityAirplane.class, "EntityAirplane", modEntityID++);
		createProjectileEntity(EntityAirplaneTakoyaki.class, "EntityAirplaneTakoyaki", modEntityID++);
	
		//register render entity
		createProjectileEntity(EntityRenderLargeShipyard.class, "EntityRenderLargeShipyard", modEntityID++);
		createProjectileEntity(EntityRenderVortex.class, "EntityRenderVortex", modEntityID++);

		//register item entity
		createItemEntity(BasicEntityItem.class, "BasicEntityItem", modEntityID++);
		
	}
	
//	//mob�۵M�ͦ���k, ������bpostInit�~�I�s, �H���o����mod���U������biome
//	public static void initNaturalSpawn() {
//		//register entity natrual spawn
//		//spawn in ALL ocean biome
//		BiomeGenBase[] allBiomes = Iterators.toArray(Iterators.filter(Iterators.forArray(BiomeGenBase.getBiomeGenArray()), Predicates.notNull()), BiomeGenBase.class);
//		
//		for(int i = 0; i < allBiomes.length; ++i) {
//			if(BiomeDictionary.isBiomeOfType(allBiomes[i], BiomeDictionary.Type.OCEAN)) {
//				EntityRegistry.addSpawn(EntityDestroyerShimakazeBoss.class, 1, 1, 1, EnumCreatureType.creature, BiomeGenBase.savanna);
//			}
//		}
//	}
	
	//�n���ͪ���k
	//�Ѽ�: �ӥͪ�class, �ͪ��W��, �Ǫ��J�I����, �Ǫ��J���I��
	public static void createEntity(Class entityClass, String entityName, int entityId){
		LogHelper.info("DEBUG : register entity: "+entityId+" "+entityClass+" "+entityName);
		//�n���Ѽ�: �ͪ�class, �ͪ��W��, �ͪ�id, mod�ƥ�, �l�ܧ�s�Z��, ��s�ɶ����j, �O�_�o�e�P�B�ʥ](���tentity����true�~�|��ܥ���)
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 48, 1, true);
	}
	
	//�n���D�ͪ���k (�L�ͩǳJ)
	//�Ѽ�: �ӥͪ�class, �ͪ��W��
	public static void createProjectileEntity(Class entityClass, String entityName, int entityId){
		//�n���Ѽ�: �ͪ�class, �ͪ��W��, �ͪ�id, mod�ƥ�, �l�ܧ�s�Z��, ��s�ɶ����j, �O�_�o�e�t�׫ʥ]
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 48, 1, true);
	}
	
	//�n��item entity��k (�L�ͩǳJ)
	//�Ѽ�: �ӥͪ�class, �ͪ��W��
	public static void createItemEntity(Class entityClass, String entityName, int entityId){
		//�n���Ѽ�: �ͪ�class, �ͪ��W��, �ͪ�id, mod�ƥ�, �l�ܧ�s�Z��, ��s�ɶ����j, �O�_�o�e�t�׫ʥ]
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 48, 1, false);
	}
	
	//�ϥΩx��@�qid�n���ͪ�
	//�Ѽ�: �ӥͪ�class, �ͪ��W��
	public static void createEntityGlobalID(Class entityClass, String entityName, int backColor, int spotColor){
		int entityId = modEntityID++;
		
		EntityRegistry.registerGlobalEntityID(entityClass, entityName, entityId);
		//�n���Ѽ�: �ͪ�class, �ͪ��W��, �ͪ�id, mod�ƥ�, �l�ܧ�s�Z��, ��s�ɶ����j, �O�_�o�e�t�׫ʥ]
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 64, 1, false);
		//�n���Ǫ��ͪ��J: �ͪ�id, �ͦ��J��T(�ͪ�id,�I����,���I��)
		EntityList.entityEggs.put(Integer.valueOf(entityId), new EntityList.EntityEggInfo(entityId, backColor, spotColor));
	}
	

}
