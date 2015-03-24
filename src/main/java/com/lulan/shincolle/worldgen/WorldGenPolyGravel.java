package com.lulan.shincolle.worldgen;

import java.util.Random;

import com.lulan.shincolle.init.ModBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

/**generate polymetallic gravel on the seabed
 * mod from clay generator
 */
public class WorldGenPolyGravel extends WorldGenerator {
	
	private Block genBlock;
    private int numberOfBlocks;
    

    public WorldGenPolyGravel(int num) {
        this.genBlock = ModBlocks.BlockPolymetalGravel;
        this.numberOfBlocks = num;
    }

    public boolean generate(World world, Random rand, int x, int y, int z) {
        if(world.getBlock(x, y, z).getMaterial() != Material.water) {
            return false;
        }
        else {
            int l = rand.nextInt(this.numberOfBlocks - 1) + 1;
            byte b0 = 1;

            //�Hx,z������, �b�|����l����
            for(int i1 = x - l; i1 <= x + l; ++i1) {
                for(int j1 = z - l; j1 <= z + l; ++j1) {
                    int k1 = i1 - x;
                    int l1 = j1 - z;

                    //check radius of cluster <= num
                    if(k1 * k1 + l1 * l1 <= l * l) {
                    	//�by����+-l���d�򤺥ͦ�
                        for(int i2 = y - b0; i2 <= y + b0; ++i2) {
                            Block block = world.getBlock(i1, i2, j1);
                            //�Y�Ӥ�����g/�F/�t��/���Y, �h���N��polymetal gravel
                            if(block == Blocks.gravel || block == Blocks.stone) {
                                world.setBlock(i1, i2, j1, this.genBlock, 0, 2);
                            }
                        }
                    }
                }
            }

            return true;
        }
    }
}
