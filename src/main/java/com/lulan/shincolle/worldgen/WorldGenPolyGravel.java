package com.lulan.shincolle.worldgen;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;

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
    	boolean notFrozen = true;
    	
    	//�I��frozen ocean, �|���������B��, �������U�����
    	if(world.getBlock(x, y - 1, z).getMaterial() == Material.ice &&
    	   world.getBlock(x, y - 2, z).getMaterial() == Material.water) {
    		Block getblock = null;
    		int newy = 1;
    		
    		//�q�ثey���U���}�l�����
    		for(newy = y - 3; newy > 3; newy--) {
    			getblock = world.getBlock(x, newy, z);
    			//���D�����, ������
    			if(getblock.getMaterial() != Material.water) {
    				y = newy;	//���w�s����
    				notFrozen = false;
    				break;
    			}
    		}
    	}
    	
    	//�Yy���פ�����O��(��ܫD����), �ΰ��׶W�L55, �h���ͦ�
        if(notFrozen && (world.getBlock(x, y, z).getMaterial() != Material.water || y > 55)) {
            return false;
        }
        else {
            int l = rand.nextInt(this.numberOfBlocks - 1) + 1;	//�ͦ��ƶq
            byte b0 = 1;										//�ͦ��p��1��polymetal

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
                            if((ConfigHandler.polyGravelBaseBlock[0] && block == Blocks.stone) ||
                               (ConfigHandler.polyGravelBaseBlock[1] && block == Blocks.gravel) ||
                               (ConfigHandler.polyGravelBaseBlock[2] && block == Blocks.sand) ||
                               (ConfigHandler.polyGravelBaseBlock[3] && block == Blocks.dirt)) {
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
