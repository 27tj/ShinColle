package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidClassic;

/**SHIP PATH FINDER
 * for ship path navigator
 * �إߪŤ� or ����path, �L�����O��B�O
 */
public class ShipPathFinder {
    /** Used to find obstacles */
    private IBlockAccess worldMap;
    /** The path being generated */
    private ShipPath path = new ShipPath();
    /** The points in the path */
    private IntHashMap pointMap = new IntHashMap();
    /** Selection of path points to add to the path */
    private ShipPathPoint[] pathOptions = new ShipPathPoint[32];
    /** is air path, for non-airplane entity */
    private boolean isPathingInAir;
    /** is airplane */
    private boolean canEntityFly;


    public ShipPathFinder(IBlockAccess block, boolean canFly) {
        this.worldMap = block;
        this.isPathingInAir = false;
        this.canEntityFly = canFly;
    }

    /**
     * Creates a path from one entity to another within a minimum distance
     */
    public ShipPathEntity createEntityPathTo(Entity fromEnt, Entity toEnt, float range) {
        return this.createEntityPathTo(fromEnt, toEnt.posX, toEnt.boundingBox.minY, toEnt.posZ, range);
    }

    /**
     * Creates a path from an entity to a specified location within a minimum distance
     */
    public ShipPathEntity createEntityPathTo(Entity entity, int x, int y, int z, float range) {
        return this.createEntityPathTo(entity, (double)((float)x + 0.5F), (double)((float)y + 0.5F), (double)((float)z + 0.5F), range);
    }

    /**
     * Internal implementation of creating a path from an entity to a point
     */
    private ShipPathEntity createEntityPathTo(Entity entity, double x, double y, double z, float range) {
        this.path.clearPath();
        this.pointMap.clearMap();
        int i;

    	i = MathHelper.floor_double(entity.boundingBox.minY + 0.5D);

        //�N�_�I���I�[�Jpoint map
        ShipPathPoint startpp = this.openPoint(MathHelper.floor_double(entity.boundingBox.minX), i, MathHelper.floor_double(entity.boundingBox.minZ));
        ShipPathPoint endpp = this.openPoint(MathHelper.floor_double(x - (double)(entity.width / 2.0F)), MathHelper.floor_double(y), MathHelper.floor_double(z - (double)(entity.width / 2.0F)));
        //�ؼЪ����e��+1�إ߬��@��path point, �Ω�P�w���|�e���O�_�|�d���entity
        ShipPathPoint entitySize = new ShipPathPoint(MathHelper.floor_float(entity.width + 1.0F), MathHelper.floor_float(entity.height + 1.0F), MathHelper.floor_float(entity.width + 1.0F));
        //�p��X�_�I���I�����Ҧ��I
        ShipPathEntity pathentity = this.addToPath(entity, startpp, endpp, entitySize, range);
        
        return pathentity;
    }

    /**
     * Adds a path from start to end and returns the whole path (args: unused, start, end, unused, maxDistance)
     */
    private ShipPathEntity addToPath(Entity entity, ShipPathPoint startpp, ShipPathPoint endpp, ShipPathPoint entitySize, float range) {
        startpp.totalPathDistance = 0.0F;
        startpp.distanceToNext = startpp.distanceToSquared(endpp);
        startpp.distanceToTarget = startpp.distanceToNext;
        this.path.clearPath();
        this.path.addPoint(startpp);
        ShipPathPoint ppTemp = startpp;
        int findCount = 0;

        /**path�إߤ�k
         * �q�_�I�}�l, 
         */
        while(!this.path.isPathEmpty()) {
        	//�qpath�����X�@�I
        	ShipPathPoint ppDequeue = this.path.dequeue();
        	findCount++;
        	//�Y���X�I=���I, �h����path
            if(ppDequeue.equals(endpp)) {						
            	//�N�Ҧ�point����path entity
//            	LogHelper.info("DEBUG : path navi: find count (pathing done) "+findCount);
                return this.createEntityPath(startpp, endpp);
            }
            
            //�Ywhile�]�Ӧh��, �j���
            if(findCount > 600) {
            	break;
            }
            
            //�Y���X�I�����I�Z����temp�I�p (�󱵪���I)
            if(ppDequeue.distanceToSquared(endpp) < ppTemp.distanceToSquared(endpp)) {
                ppTemp = ppDequeue;	//�Ntemp�I�]�����X�I
            }
            
            //�N���X�I�аO���_�I, �P�w���I���ؼЪ��i����V������
            ppDequeue.isFirst = true;
            int i = this.findPathOptions(entity, ppDequeue, entitySize, endpp, range); //���o�i����V
            //�N�Ҧ��i����V�����ը��ݬ�
            for(int j = 0; j < i; ++j) {
            	ShipPathPoint ppTemp2 = this.pathOptions[j];
                float f1 = ppDequeue.totalPathDistance + ppDequeue.distanceToSquared(ppTemp2);
                //�Y���I�٨S�[�J�Lpath, �θ��I�w�[�J��path�������I���e��X�Ӫ�path���׸���, �h�ݭn�A��s�@��path����
                if(!ppTemp2.isAssigned() || f1 < ppTemp2.totalPathDistance) {
                	//�N���I�����[�J��path��, �]�w��e���I��path���׭�
                    ppTemp2.previous = ppDequeue;		//�e�@�I�]�����X�I
                    ppTemp2.totalPathDistance = f1;		//�s�U��path���׭�
                    ppTemp2.distanceToNext = ppTemp2.distanceToSquared(endpp);	//�U�@�I�]�����I
                    //�Y���I���ӴN�bpath��, �h��s��path���׭�
                    if(ppTemp2.isAssigned()) {
                        this.path.changeDistance(ppTemp2, ppTemp2.totalPathDistance + ppTemp2.distanceToNext);
                    }
                    else {	//���bpath��, �����[�Jpath
                        ppTemp2.distanceToTarget = ppTemp2.totalPathDistance + ppTemp2.distanceToNext;
                        this.path.addPoint(ppTemp2);
                    }
                }
            }
        }
        
        if(ppTemp == startpp) {	//�Y�����䤣���I�i�H�[�J, �h�^��null, ��ܧ䤣��path
//        if(this.path.getCount() <= 0) {	//�Y�����䤣���I�i�H�[�J, �h�^��null, ��ܧ䤣��path
//        	LogHelper.info("DEBUG : path navi: no path");
            return null;
        }
        else {					//�^��path entity
        	//�N�Ҧ�point����path entity
        	LogHelper.info("DEBUG : path navi: find count (pathing fail, cannot reach, find count = "+findCount+" times) ");
            return this.createEntityPath(startpp, ppTemp);
        }
    }

    /**�P�w���I����I��path���X�Ӥ�V�i��
     * populates pathOptions with available points and returns the number of options found (args: unused1, currentPoint,
     * unused2, targetPoint, maxDistance)
     */
    private int findPathOptions(Entity entity, ShipPathPoint currentpp, ShipPathPoint entitySize, ShipPathPoint targetpp, float range) {
        int i = 0;
        byte pathCase = 0;	

        //�Y�Y���S�d��F��, �hpathCase = 1, ��ܥi���W1�����|
        int j = this.getVerticalOffset(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize);
        if(j == 1 || j == 3) {
            pathCase = 1;
        }
        //���d���k�e��|���I�����p: �U->���k�e��->�W
        ShipPathPoint pathpoint2 = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord - 1, currentpp.zCoord, entitySize, pathCase);
        ShipPathPoint pathpoint3 = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord + 1, entitySize, pathCase);
        ShipPathPoint pathpoint4 = this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord, entitySize, pathCase);
        ShipPathPoint pathpoint5 = this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord, entitySize, pathCase);
        ShipPathPoint pathpoint6 = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord - 1, entitySize, pathCase);
        ShipPathPoint pathpoint7 = null;
        //�W�����G�����~���\���W����|, �Y�W�����Ů�, �h�u���i�H�����~���W����|
        if(j == 3 || (j == 1 && this.canEntityFly)) {
        	pathpoint7 = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize, pathCase);
        }       
        
        //�Y���I���w���I, �B���O�_�l�I, �B�b�M���̤j�d��
        if(pathpoint2 != null && !pathpoint2.isFirst && pathpoint2.distanceTo(targetpp) < range) {
//        	LogHelper.info("DEBUG : path navi: down path find: "+pathpoint2.yCoord);
        	this.pathOptions[i++] = pathpoint2;	//�[�J��i��ﶵ��
        }
        if(pathpoint3 != null && !pathpoint3.isFirst && pathpoint3.distanceTo(targetpp) < range) {
//        	LogHelper.info("DEBUG : path navi: horz path A find: "+pathpoint3.yCoord);
        	this.pathOptions[i++] = pathpoint3;	//�[�J��i��ﶵ��
        }
        if(pathpoint4 != null && !pathpoint4.isFirst && pathpoint4.distanceTo(targetpp) < range) {
            this.pathOptions[i++] = pathpoint4;
        }
        if(pathpoint5 != null && !pathpoint5.isFirst && pathpoint5.distanceTo(targetpp) < range) {
            this.pathOptions[i++] = pathpoint5;
        }
        if(pathpoint6 != null && !pathpoint6.isFirst && pathpoint6.distanceTo(targetpp) < range) {
            this.pathOptions[i++] = pathpoint6;
        }
        if(pathpoint7 != null && !pathpoint7.isFirst && pathpoint7.distanceTo(targetpp) < range) {
            this.pathOptions[i++] = pathpoint7;
        }

        return i;
    }

    /**����w�I(x,y,z)�O�_�i�w�����ʹL�h, �]�A���I�U���O�_�i�w������ (change: �ק���򩥼ߤ]��i���ߤ��)
     * Returns a point that the entity can safely move to
     * pathOption:  0:blocked  1:clear
     */
    private ShipPathPoint getSafePoint(Entity entity, int x, int y, int z, ShipPathPoint entitySize, int pathOption) {
    	ShipPathPoint pathpoint1 = null;
        int pathCase = this.getVerticalOffset(entity, x, y, z, entitySize);	//���o���I���p
        
        if(pathCase == 2 || pathCase == 3) {//�Y���I���p��: open trapdoor or �G��, �h������safe point
            return this.openPoint(x, y, z);	//�[�J���I��path
        }
        else {				//��L���p
        	//�Y���p��clear, �h����I�[�J��path
            if(pathCase == 1) {
                pathpoint1 = this.openPoint(x, y, z);
            }
            //�Y���p�Dclear�ӬO������, �B���\���W����|, �h���W��
            if(pathpoint1 == null && pathOption > 0 && pathCase != -3 && pathCase != -4 && 
               this.getVerticalOffset(entity, x, y + pathOption, z, entitySize) == 1) {
                pathpoint1 = this.openPoint(x, y + pathOption, z);	//�⩹�W�@�I�[�J��path
                y += pathOption;
            }
            //�Y�����i�[�Jpath���I, �h���U����I���U����O�_�i�w������(�u���U��X��, X�̷�entity�ۤv��getMaxSafePointTries�M�w)
            if(pathpoint1 != null) {
            	//�Y�i�H��, �h�����^��pathpoint
            	if(this.canEntityFly) {
            		return pathpoint1;
            	}
            	
            	//�Y���୸, �h���U��w�����a�I
                int j1 = 0;
                int downCase = 0;
                
                while(y > 0) {
                    downCase = this.getVerticalOffset(entity, x, y - 1, z, entitySize);
                    //�Y���U�������G��(����b�Ť�, �]���|����G������), �hpath�I���קאּ�b�G�餤
                    if(downCase == 3) {
                    	pathpoint1 = this.openPoint(x, y - 1, z);
                        break;
                    }
                    
                    //�Y�L�k���U, ��ܤw�g���a, �h�H���I�@��path�I
                    if(downCase != 1) {
                        break;
                    }
                    
                    //�Y���նW�L�S�w����, �h�P�w�S���w�����a, �Ǧ^null
                    if(j1++ >= 32) {
                        return null;
                    }
                    
                    //���U�丨�a�I, �N���a�I�[�J��path
                    --y;
                    if(y > 0) {
                        pathpoint1 = this.openPoint(x, y, z);
                    }
                }
            }
            
//            if(pathCase == -3) {
//            	LogHelper.info("DEBUg : find fence "+x+" "+y+" "+z+" "+pathpoint1);
//            }
            
            return pathpoint1;
        }
    }

    /**
     * Returns a mapped point or creates and adds one
     */
    private final ShipPathPoint openPoint(int x, int y, int z) {
        int l = ShipPathPoint.makeHash(x, y, z);	//�y�ЭȺ�hash
        ShipPathPoint pathpoint = (ShipPathPoint)this.pointMap.lookup(l);	//��hash�ȧ���|���O�_�����I

        //path���䤣����I, �h�إߤ�
        if(pathpoint == null) {
            pathpoint = new ShipPathPoint(x, y, z);
            this.pointMap.addKey(l, pathpoint);
        }

        return pathpoint;
    }

    /**NEW:
     * 3:  liquid (water, lava, forge liquid)
     * 2:  open trapdoor
     * 1:  clear(air)
     * 0:  solid block
     * -3: fence or tracks
     * -4: closed trap door
     * 
     * ORIGIN:
     * Checks if an entity collides with blocks at a position. Returns 1 if clear, 0 for colliding with any solid block,
     * -1 for water(if avoiding water) but otherwise clear, -2 for lava, -3 for fence, -4 for closed trapdoor, 2 if
     * otherwise clear except for open trapdoor or water(if not avoiding)
     */
    public int getVerticalOffset(Entity entity, int x, int y, int z, ShipPathPoint entitySize) {
//    	return func_82565_a(entity, x, y, z, point, this.isPathingInWater, this.isMovementBlockAllowed, this.isWoddenDoorAllowed);
        return func_82565_a(entity, x, y, z, entitySize, this.isPathingInAir);
    }

    public static int func_82565_a(Entity entity, int x, int y, int z, ShipPathPoint entitySize, boolean inAir) {
        boolean pathToDoor = false;		//�S�w����i�q�L��flag: ������
        boolean pathInLiquid = true;	//�O�_�O���b�G�餤��path (�Y����y�����G����)
        
        //�P�w��point�[�Wentity�����e����, �O�_�|�I�����L���
        for(int l = x; l < x + entitySize.xCoord; ++l) {
            for(int i1 = y; i1 < y + entitySize.yCoord; ++i1) {
                for(int j1 = z; j1 < z + entitySize.zCoord; ++j1) {
                	Block block = entity.worldObj.getBlock(l, i1, j1);

                    //�Y�I����D�Ů�, ��, ���ߤ��
                    if(block.getMaterial() != Material.air) {
                    	//�ˬd����y�O�_�����G��
                        if(pathInLiquid && i1 == y && !EntityHelper.checkBlockIsLiquid(block)) {
                        	pathInLiquid = false;
                        }
                        
//                    	//�I�쪺�O������
//                        if(block == Blocks.trapdoor) {
//                        	pathToDoor = true;	//�����S�w���p�~��q�L
//                        }

                        int k1 = block.getRenderType();
                        
                        //�Y�I�줣���L�����: ��������, ������, �]���
                        if(!block.getBlocksMovement(entity.worldObj, l, i1, j1)) {
                        	//�Y�b�]��, ���, �]��������, �^��-3
                            if(k1 == 11 || block == Blocks.fence_gate || k1 == 32) {
                                return -3;
                            }
                            //�Y�O������������, �^��-4
                            if(block == Blocks.trapdoor) {
                                return -4;
                            }
                            //�Y�D�y����, �h���P�w��0
                            if(!EntityHelper.checkBlockIsLiquid(block)) {
                                return 0;
                            }
                        }
                    }
                    else {	//�Y��air���
                    	//�ˬd����y�O�_���G��
                        if(pathInLiquid && i1 == y) {
                        	pathInLiquid = false;
                        }
                    }
                }
            }
        }

        return pathInLiquid ? 3 : pathToDoor ? 2 : 1;
    }

    /**
     * Returns a new PathEntity for a given start and end point
     */
    private ShipPathEntity createEntityPath(ShipPathPoint startpp, ShipPathPoint endpp) {
        int i = 1;
        ShipPathPoint pathpoint2;

        //��Xpath�`�I��, �s��i
        for(pathpoint2 = endpp; pathpoint2.previous != null; pathpoint2 = pathpoint2.previous) {
            ++i;
        }

        ShipPathPoint[] pathtemp = new ShipPathPoint[i];
        pathpoint2 = endpp;
        --i;

        //�Npath�Ҧ��I�s��pathtemp
        for(pathtemp[i] = endpp; pathpoint2.previous != null; pathtemp[i] = pathpoint2) {
            pathpoint2 = pathpoint2.previous;
            --i;
        }

        return new ShipPathEntity(pathtemp);
    }
}
