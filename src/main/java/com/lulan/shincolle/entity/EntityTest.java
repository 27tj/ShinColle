package com.lulan.shincolle.entity;

import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public class EntityTest extends EntityLiving {

	public EntityTest(World world) {
		super(world);
		this.setSize(1F, 1.5F);	//�I���j�p ��ҫ��j�p�L��
	}

}
