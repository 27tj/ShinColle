package com.lulan.shincolle.init;

import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;

import com.lulan.shincolle.ShinColle;
import com.lulan.shincolle.entity.EntityDestroyerI;
import com.lulan.shincolle.reference.Reference;

import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

public class ModEntity {
	

	public static void init() {
		createEntity(EntityDestroyerI.class, Reference.MOD_ID_LOW+":EntityDestroyerI", 0xEC4545, 0x001EFF);
		
	}
	
	//�n���ͪ���k
	//�Ѽ�: �ӥͪ�class, �ͪ��W��, �Ǫ��J�I����, �Ǫ��J���I��
	public static void createEntity(Class entityClass, String entityName, int backColor, int spotColor){
		int entityId = EntityRegistry.findGlobalUniqueEntityId();	//��@�ӪŪ��ͪ�id�ӥ�		
		
		EntityRegistry.registerGlobalEntityID(entityClass, entityName, entityId);
		//�n���Ѽ�: �ͪ�class, �ͪ��W��, �ͪ�id, mod�ƥ�, �l�ܧ�s�Z��, ��s�ɶ����j, �O�_�o�e�t�׫ʥ]
		EntityRegistry.registerModEntity(entityClass, entityName, entityId, ShinColle.instance, 128, 1, true);
		//�n���Ǫ��ͪ��J: �ͪ�id, �ͦ��J��T(�ͪ�id,�I����,���I��)
		EntityList.entityEggs.put(Integer.valueOf(entityId), new EntityList.EntityEggInfo(entityId, backColor, spotColor));

	}
	

}
