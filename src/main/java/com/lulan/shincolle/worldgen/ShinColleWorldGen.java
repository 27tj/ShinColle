package com.lulan.shincolle.worldgen;

import java.util.Random;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraftforge.common.BiomeDictionary;
import cpw.mods.fml.common.IWorldGenerator;

public class ShinColleWorldGen implements IWorldGenerator {
	
	private WorldGenerator genPolymetal, genPolyGravel;

	//���קP�w
	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world,
			IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
		//�̷Ӻ���id�I�s���P�ͦ���k
		switch(world.provider.dimensionId) {
		case 0:		//�@��@��
			generateSurface(world, random, chunkX*16, chunkZ*16);	//�Nchunk��mx16 �নblock��m
			generateSea(world, random, chunkX*16, chunkZ*16);
			break;
		case -1:	//�a��
		//	generateNether(world, random, chunkX*16, chunkZ*16);
			break;
		case 1:		//�׬�
		//	generateEnd(world, random, chunkX*16, chunkZ*16);
			break;
		default:	//��L����
			generateSurface(world, random, chunkX*16, chunkZ*16);
			generateSea(world, random, chunkX*16, chunkZ*16);
			break;		
		}		
	}

	//�H���ͦ���
	//�Ѽ�: �q��,�ͦ��@��,�H����,x�_�I,z�_�I,�ͦ�����,�̧C����,�̰�����
	//�ͦ�����:�K/����~10 �p��/��~2
	private void oreGenerator(WorldGenerator genOres, World world, Random rand, int blockX, int blockZ, int spawnNum, int minY, int maxY) {	
		//NYI: �̷ӥͺA�tid�ͦ����P�ƶq���q
		//�H�_�IblockX,blockZ�H���[�W0~15(�Y�@��chunk�d��)  �ͦ����׫h��minY~maxY����
		//�C��chunk����spawnChance���ͦ��ʧ@
		int x,y,z = 0;
		int spawnN = spawnNum;
		
		BiomeGenBase biome = world.getBiomeGenForCoords(blockX, blockZ);
		
		if(BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN)) {
			spawnN *= 3;
		}
		
		for(int i = 0; i < spawnN; i++) {
			x = blockX + rand.nextInt(16);
			z = blockZ + rand.nextInt(16);
			y = minY + rand.nextInt(maxY - minY);
			genOres.generate(world, rand, x, y, z);
		}
	}

	//�@��@�ɥͦ���k  �C��chunk���|�I�s�@��
	private void generateSurface(World world, Random rand, int x, int z) {
		//Polymetal�ͦ�: �ͦ��j�p4~8��block �Cchunk�ͦ�����10�� �ͦ�����2~40
		genPolymetal = new WorldGenMinable(ModBlocks.BlockPolymetalOre, 4 + rand.nextInt(4));  //�C��chunk�|���s�H���@���ͦ��q���j�p
		oreGenerator(genPolymetal, world, rand, x, z, ConfigHandler.polyOreBaseRate, 3, 50);
		
	}
	
	//���v�����ͺA�t: polymetallic gravel�ͦ���k
	private void generateSea(World world, Random rand, int x, int z) {
		BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
		
		if(BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN)) {
			genPolyGravel = new WorldGenPolyGravel(2 + rand.nextInt(2));
			int posX, posY, posZ = 0;
			
			for(int i = 0; i < ConfigHandler.polyGravelBaseRate; i++) {
				posX = x + rand.nextInt(16);
				posZ = z + rand.nextInt(16);
				posY = world.getTopSolidOrLiquidBlock(posX, posZ);	//���o���׳̰���������+1��(�Y����+1)
				
				genPolyGravel.generate(world, rand, posX, posY, posZ);
			}
		}
	}
	
/*
	//�a���ͦ���k  �C��chunk���|�I�s�@��
	private void generateNether(World world, Random rand, int x, int z) {
				
	}

	//�׬ɥͦ���k  �C��chunk���|�I�s�@��
	private void generateEnd(World world, Random rand, int x, int z) {
			
	}
	
*/	
}
