package com.lulan.shincolle.ai.path;

import com.lulan.shincolle.entity.IShipNavigator;
import com.lulan.shincolle.utility.EntityHelper;
import com.lulan.shincolle.utility.FormatHelper;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;

/**SHIP PATH NAVIGATE
 * ship or airplane���wpath ai, ��entity������@IShipNavigator
 * �L�����O�Ϊ̯B�O�@�Χ�X�Ť� or �������|, �Yentity�b���W���ʫh���ݭn��s��navigator
 * update move���������ĥΦ۵M�Y����jump, �ӬO�����[�W�@��motionY
 * �`�N��path navigator�ϥή�, ������ship floating�����H�K��êy�b����
 */
public class ShipPathNavigate {
    private EntityLiving theEntity;
    /** The entity using ship path navigate */
    private IShipNavigator theEntity2;
    private World worldObj;
    /** The PathEntity being followed. */
    private ShipPathEntity currentPath;
    private double speed;
    /** The number of blocks (extra) +/- in each axis that get pulled out as cache for the pathfinder's search space */
    private IAttributeInstance pathSearchRange;
    /** Time, in number of ticks, following the current path */
    private int totalTicks;
    /** The time when the last position check was done (to detect successful movement) */
    private int ticksAtLastPos;
    /** Coordinates of the entity's position last time a check was done (part of monitoring getting 'stuck') */
    private Vec3 lastPosCheck = Vec3.createVectorHelper(0.0D, 0.0D, 0.0D);
    /** entity is airplane flag*/
    private boolean canFly;
    

    public ShipPathNavigate(EntityLiving entity, World world) {
        this.theEntity = entity;
        this.theEntity2 = (IShipNavigator) entity;
        this.worldObj = world;
        this.pathSearchRange = entity.getEntityAttribute(SharedMonsterAttributes.followRange);
        this.canFly = false;
    }

    /**
     * Sets the speed
     */
    public void setSpeed(double par1) {
        this.speed = par1;
    }
    
    public void setCanFly(boolean par1) {
    	this.canFly = par1;
    }
    
    public boolean getCanFly() {
    	return this.canFly;
    }

    /**
     * Gets the maximum distance that the path finding will search in.
     */
    public float getPathSearchRange() {
        return (float)this.pathSearchRange.getAttributeValue();
    }
    
    /**
     * Try to find and set a path to XYZ. Returns true if successful.
     */
    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        ShipPathEntity pathentity = this.getPathToXYZ((double)MathHelper.floor_double(x), (double)((int)y), (double)MathHelper.floor_double(z));
        return this.setPath(pathentity, speed);
    }

    /**
     * Returns the path to the given coordinates
     */
    public ShipPathEntity getPathToXYZ(double x, double y, double z) {
        return !this.canNavigate() ? null : this.getShipPathToXYZ(this.theEntity, MathHelper.floor_double(x), (int)y, MathHelper.floor_double(z), this.getPathSearchRange(), this.canFly);
    }
    
    public ShipPathEntity getShipPathToXYZ(Entity entity, int x, int y, int z, float range, boolean canFly) {
        this.worldObj.theProfiler.startSection("pathfind");
        //xyz1����l��m  xyz2�����d�� xyz3���k�d��
        //�Y�Nentity��m, ������V�X�irange+8��, �b���d�򤺭p����|
        int x1 = MathHelper.floor_double(entity.posX);
        int y1 = MathHelper.floor_double(entity.posY);
        int z1 = MathHelper.floor_double(entity.posZ);
        int range1 = (int)(range + 8.0F);
        int x2 = x1 - range1;
        int y2 = y1 - range1;
        int z2 = z1 - range1;
        int x3 = x1 + range1;
        int y3 = y1 + range1;
        int z3 = z1 + range1;
        ChunkCache chunkcache = new ChunkCache(this.worldObj, x2, y2, z2, x3, y3, z3, 0);
        ShipPathEntity pathentity = (new ShipPathFinder(chunkcache, canFly)).createEntityPathTo(entity, x, y, z, range);
        this.worldObj.theProfiler.endSection();
        return pathentity;
    }

    /**
     * Returns the path to the given EntityLiving
     */
    public ShipPathEntity getPathToEntityLiving(Entity entity) {
//        return !this.canNavigate() ? null : this.worldObj.getPathEntityToEntity(this.theEntity, entity, this.getPathSearchRange(), this.canPassOpenWoodenDoors, this.canPassClosedWoodenDoors, this.avoidsWater, this.canSwim);
        return !this.canNavigate() ? null : this.getPathEntityToEntity(this.theEntity, entity, this.getPathSearchRange(), this.canFly);
    }
    
    public ShipPathEntity getPathEntityToEntity(Entity entity, Entity targetEntity, float range, boolean canFly) {
        this.worldObj.theProfiler.startSection("pathfind");
        //xyz1����l��m  xyz2�����d�� xyz3���k�d��
        //�Y�Nentity��m, ������V�X�irange+8��, �b���d�򤺭p����|
        int x1 = MathHelper.floor_double(entity.posX);
        int y1 = MathHelper.floor_double(entity.posY + 1.0D);
        int z1 = MathHelper.floor_double(entity.posZ);
        int range1 = (int)(range + 16.0F);
        int x2 = x1 - range1;
        int y2 = y1 - range1;
        int z2 = z1 - range1;
        int x3 = x1 + range1;
        int y3 = y1 + range1;
        int z3 = z1 + range1;
        ChunkCache chunkcache = new ChunkCache(this.worldObj, x2, y2, z2, x3, y3, z3, 0);
        ShipPathEntity pathentity = (new ShipPathFinder(chunkcache, canFly)).createEntityPathTo(entity, targetEntity, range);
        this.worldObj.theProfiler.endSection();
        return pathentity;
    }

    /**
     * Try to find and set a path to EntityLiving. Returns true if successful.
     */
    public boolean tryMoveToEntityLiving(Entity entity, double speed) {
        ShipPathEntity pathentity = this.getPathToEntityLiving(entity);
        return pathentity != null ? this.setPath(pathentity, speed) : false;
    }

    /**
     * sets the active path data if path is 100% unique compared to old path, checks to adjust path for sun avoiding
     * ents and stores end coords
     */
    public boolean setPath(ShipPathEntity pathEntity, double speed) {
        //�Y���|��null, ��ܧ䤣����|
    	if(pathEntity == null) {
            this.currentPath = null;
            return false;
        }
        else {
        	//����s�¸��|�O�_�ۦP, ���P�ɱN�¸��|�\��
            if(!pathEntity.isSamePath(this.currentPath)) {
                this.currentPath = pathEntity;
            }
            //�Y���|���׬�0, ��ܨSpath
            if(this.currentPath.getCurrentPathLength() == 0) {
                return false;
            }
            else {	//���\�]�wpath
                this.speed = speed;
                Vec3 vec3 = this.getEntityPosition();
                this.ticksAtLastPos = this.totalTicks;
                this.lastPosCheck.xCoord = vec3.xCoord;
                this.lastPosCheck.yCoord = vec3.yCoord;
                this.lastPosCheck.zCoord = vec3.zCoord;
                return true;
            }
        }
    }

    /**
     * gets the actively used PathEntity
     */
    public ShipPathEntity getPath() {
        return this.currentPath;
    }

    /** navigation tick */
    public void onUpdateNavigation() {
        ++this.totalTicks;
        //�Y��path
        if(!this.noPath()) {
            //�YpathFollow�S��path�M��, ����٥i�H�~�򲾰�
        	//���o�U�@�ӥؼ��I
            Vec3 vec3 = this.currentPath.getPosition(this.theEntity);
//                LogHelper.info("DEBUG : path navi: path vec "+this.currentPath.getCurrentPathIndex()+" / "+this.currentPath.getCurrentPathLength()+" "+vec3.xCoord+" "+vec3.yCoord+" "+vec3.zCoord);
//                LogHelper.info("DEBUG : path navi: path pp  "+currentPath.getPathPointFromIndex(currentPath.getCurrentPathIndex()).xCoord+" "+currentPath.getPathPointFromIndex(currentPath.getCurrentPathIndex()).yCoord+" "+currentPath.getPathPointFromIndex(currentPath.getCurrentPathIndex()).zCoord+" ");
//                LogHelper.info("DEBUG : path navi: path pos "+this.theEntity.posX+" "+this.theEntity.posY+" "+this.theEntity.posZ+" ");
            //�Y�٦��U�@���I�n����, �h�]�w���ʶq��move helper�i�H��ڲ���entity
            if(vec3 != null) {
                this.theEntity2.getShipMoveHelper().setMoveTo(vec3.xCoord, vec3.yCoord, vec3.zCoord, this.speed);
            }
            
            //�Y�i�H���沾��, �h�]pathFollow��k��s�U�@�ӥؼ��I
            if(this.canNavigate()) {
//            	LogHelper.info("DEBUG : path navi: path follow");
                this.pathFollow();
            }
        }
    }

    /** �P�wentity�O�_�d��(�W�L100 tick���b��a) or entity�O�_�i�H�۱��|�H�ٲ��@�Ǹ��|�I 
     *  �Hy���קP�w�O�_�����I�i�H�ٲ� (���P�w�����Z��)
     */
    private void pathFollow() {
        Vec3 entityPos = this.getEntityPosition();
        int pptemp = this.currentPath.getCurrentPathLength();

//        //���y�٨S�������|�I, ��X�O�_��y���׮t�Z����1�檺�I
//        for(int j = this.currentPath.getCurrentPathIndex(); j < this.currentPath.getCurrentPathLength(); ++j) {
////            if(this.currentPath.getPathPointFromIndex(j).yCoord != (int)entityPos.yCoord) {
//        	float diff = (float) (this.currentPath.getPathPointFromIndex(j).yCoord - entityPos.yCoord);
//        	if(MathHelper.abs(diff) > 0.8F) {
//            	pptemp = j;
////            	LogHelper.info("DEBUG : path navi: y diff "+j+" "+diff+" : "+this.currentPath.getPathPointFromIndex(j).yCoord+" "+entityPos.yCoord);
////            	LogHelper.info("DEBUG : path navi: y diff "+j+" "+" : "+this.currentPath.getPathPointFromIndex(j).yCoord+" "+entityPos.yCoord);
//            	break;
//            }
//        }

//        float widthSq = this.theEntity.width * this.theEntity.width * 0.25F;
        float widthSq = this.theEntity.width * this.theEntity.width;
        int k;

        //���y�ثe���I��y���פ��Por�̫᪺�I, �Y�Z������entity�j�p, �h�P�wentity�w�g��F���I
        for(k = this.currentPath.getCurrentPathIndex(); k < pptemp; ++k) {
            if(entityPos.squareDistanceTo(this.currentPath.getVectorFromIndex(this.theEntity, k)) < (double)widthSq) {
//            	LogHelper.info("DEBUG : path navi: get path+1 "+k+" "+entityPos.squareDistanceTo(this.currentPath.getVectorFromIndex(this.theEntity, k)));
            	this.currentPath.setCurrentPathIndex(k + 1);	//�w��F�ؼ��I, �]�w�ؼ��I���U�@�I
            }
        }

        k = MathHelper.ceiling_float_int(this.theEntity.width);
        int heighInt = (int)this.theEntity.height;
        int widthInt = k;

        //�qy���פ��X���I���^���y, ��O�_���I��q�ثe�I���u���L�h, �����ܱN���I�]���ؼ��I��entity�����I���u�e�i
        for(int j1 = pptemp - 1; j1 >= this.currentPath.getCurrentPathIndex(); --j1) {
            if(this.isDirectPathBetweenPoints(entityPos, this.currentPath.getVectorFromIndex(this.theEntity, j1), k, heighInt, widthInt)) {
                this.currentPath.setCurrentPathIndex(j1);
                break;
            }
        }

        //�CN ticks�ˬd�@�����ʶZ��
        if(this.totalTicks - this.ticksAtLastPos > 25) {
        	//�Y�Z���W�@�����\���ʪ��I����1.5��, �h��ܬY�ح�]�y���X�G�S����, �M����path
            if(entityPos.squareDistanceTo(this.lastPosCheck) < 2.25D) {
            	//�i�ॻ���b�]������(�_�I�N�b�]����), �P�w�n���ʨ�]��j���Ŧa���, ���O�n��L�]��, AI�L�k�P�_�H�b�]����@��
            	//�ȩw�Ѫk: �H�������|��V�����k���ʤ@��, ���ղ����]��
//            	Block stand = theEntity.worldObj.getBlock(MathHelper.floor_double(theEntity.posX), (int)theEntity.posY, MathHelper.floor_double(theEntity.posZ));
            	float dx = (float) (theEntity.posX - currentPath.getVectorFromIndex(this.theEntity, currentPath.getCurrentPathIndex()).xCoord);
            	float dz = (float) (theEntity.posZ - currentPath.getVectorFromIndex(this.theEntity, currentPath.getCurrentPathIndex()).zCoord);
            	LogHelper.info("DEBUG : path navi: get stand block: "+dx+" "+dz);
            	double targetX = theEntity.posX;
            	double targetZ = theEntity.posZ;
            	
            	//get random position
            	if(dx > 0.2 || dx < -0.2) {	//�Y�ؼ��I��x��V�@�w�Z��, �h�bz��V�H����+-1
            		targetZ = theEntity.getRNG().nextInt(2) == 0 ? targetZ - 1.5D : targetZ + 1.5D;
            	}
            	
            	if(dz > 0.2 || dz < -0.2) {
            		targetX = theEntity.getRNG().nextInt(2) == 0 ? targetX - 1.5D : targetX + 1.5D;
            	}
            	
            	//set move
            	this.theEntity2.getShipMoveHelper().setMoveTo(targetX, theEntity.posY, targetZ, this.speed);
            
            	//�W�L5��L�k����, clear path
                if(this.totalTicks - this.ticksAtLastPos > 100) {
                	this.clearPathEntity();
                }
            }

            //��s���\���ʪ�����
            this.ticksAtLastPos = this.totalTicks;
            this.lastPosCheck.xCoord = entityPos.xCoord;
            this.lastPosCheck.yCoord = entityPos.yCoord;
            this.lastPosCheck.zCoord = entityPos.zCoord;
        }
    }

    /**
     * If null path or reached the end
     */
    public boolean noPath() {
        return this.currentPath == null || this.currentPath.isFinished();
    }

    /**
     * sets active PathEntity to null
     */
    public void clearPathEntity() {
        this.currentPath = null;
    }

    /** 
     * �Nentity��m��T�Hvec3���
     */
    private Vec3 getEntityPosition() {
//        return Vec3.createVectorHelper(this.theEntity.posX, this.getPathableYPos(), this.theEntity.posZ);
        return Vec3.createVectorHelper(this.theEntity.posX, this.theEntity.posY, this.theEntity.posZ);
    }

    /**CHANGE:
     * �Y�୸, �h�H�ثey���_�I
     * �Y���୸, �h���U���Ĥ@�ӫD�Ů𪺤�����_�I (���� or ������)
     * 
     * ORIGIN:
     * �Y���a, �h��X��������������y�@��path�_�I
     * �Y�����a, �h�H�ثey�@��path�_�I
     * Gets the safe pathing Y position for the entity depending on if it can path swim or not
     */
    private double getPathableYPos() {
    	if(this.canFly) {
//    	LogHelper.info("DEBUG : path navi: path y "+(int)(this.theEntity.boundingBox.minY + 0.5D));
    		//�i�H��, �����H�ثey���_�I
//            return (int)(this.theEntity.boundingBox.minY + 0.5D);
    		return this.theEntity.posY;
        }
        else {
        	 int i = (int)this.theEntity.boundingBox.minY;
             Block block = this.worldObj.getBlock(MathHelper.floor_double(this.theEntity.posX), i, MathHelper.floor_double(this.theEntity.posZ));
             int j = 0;
             //���U��X�Ĥ@�ӫDair�����
             do {
                 if(block != Blocks.air && block != null) {
                	 if(EntityHelper.checkBlockIsLiquid(block)) {
                		 return i + 0.4D;
                	 }
                	 else {
                		 return i;
                	 }  
                 }

                 ++i;
                 block = this.worldObj.getBlock(MathHelper.floor_double(this.theEntity.posX), i, MathHelper.floor_double(this.theEntity.posZ));
                 ++j;
             }
             while (j <= 16);	//�̦h���U��16��N����
             //��W�L16�泣�S��, �h�����^�ǥثey
             return (int)this.theEntity.boundingBox.minY;
        }
    }

    /**�D�M����, �B��entity�i�H�� or ���b�a�� or �b�i��z�����
     */
    private boolean canNavigate() {
        return !theEntity.isRiding() && (this.canFly || this.theEntity.onGround || EntityHelper.checkEntityIsFree(theEntity));
    }

    /**
     * Returns true if the entity is in water or lava or other liquid
     */
    private boolean isInLiquid() {
        return EntityHelper.checkEntityIsInLiquid(theEntity);
    }

    /**NO USE
     * Trims path data from the end to the first sun covered block
     */
    private void removeSunnyPath() {
        if(!this.worldObj.canBlockSeeTheSky(MathHelper.floor_double(this.theEntity.posX), (int)(this.theEntity.boundingBox.minY + 0.5D), MathHelper.floor_double(this.theEntity.posZ))) {
            for(int i = 0; i < this.currentPath.getCurrentPathLength(); ++i) {
                ShipPathPoint pathpoint = this.currentPath.getPathPointFromIndex(i);

                if(this.worldObj.canBlockSeeTheSky(pathpoint.xCoord, pathpoint.yCoord, pathpoint.zCoord)) {
                    this.currentPath.setCurrentPathLength(i - 1);
                    return;
                }
            }
        }
    }

    /**entity�۱��|����k, �Y���u�i���L��ê, �h�|���u�����I����
     * NEW: �[�Jy�b�P�w, �Ϩ�i�H��y�b���|
     * Returns true when an entity of specified size could safely walk in a straight line between the two points. Args:
     * pos1, pos2, entityXSize, entityYSize, entityZSize
     */
    private boolean isDirectPathBetweenPoints(Vec3 pos1, Vec3 pos2, int xSize, int ySize, int zSize) {
        int x1 = MathHelper.floor_double(pos1.xCoord);
        int z1 = MathHelper.floor_double(pos1.zCoord);
        int y1 = (int)pos1.yCoord;
        double xOffset = pos2.xCoord - pos1.xCoord;
        double zOffset = pos2.zCoord - pos1.zCoord;
        double yOffset = pos2.yCoord - pos1.yCoord;
        double xzOffsetSq = xOffset * xOffset + zOffset * zOffset + yOffset * yOffset;
//        double xzOffsetSq = xOffset * xOffset + zOffset * zOffset;

        if(xzOffsetSq < 1.0E-12D) {	//�Y�Z�����p, �h�P�w�������I (���ʳ̤p���O0.01 �]��xzSq�̤p��E-8)
            return false;
        }
        else {						//�j�M�i�����I
            double xzOffset = 1.0D / Math.sqrt(xzOffsetSq);
            xOffset *= xzOffset;	//normalize offset
            zOffset *= xzOffset;
            yOffset *= xzOffset;
            xSize += 2;				//size�X�i2, �H�ˬd�P����
            zSize += 2;
            ySize += 2;
            
            if(!this.isSafeToStandAt(x1, y1, z1, xSize, ySize, zSize, pos1, xOffset, zOffset)) {
                return false;		//�Y���I����w������, �hfalse
            }
            else {					//���I�i�w������
            	//�Y�^��size
                xSize -= 2;
                zSize -= 2;
                ySize -= 2; 
                //offset�������
                double xOffAbs = 1.0D / Math.abs(xOffset);
                double zOffAbs = 1.0D / Math.abs(zOffset);
                double yOffAbs = 1.0D / Math.abs(yOffset);  
                //int�y��-double�y��, ���o�첾theta��
                double x1Theta = (double)(x1*1) - pos1.xCoord;
                double z1Theta = (double)(z1*1) - pos1.zCoord;
                double y1Theta = (double)(y1*1) - pos1.yCoord;     
                //�Yoffset�����V, �htheta+1�ܦ^����
                if(xOffset >= 0.0D) {
                    ++x1Theta;
                }
                if(zOffset >= 0.0D) {
                    ++z1Theta;
                }
                if(yOffset >= 0.0D) {
                    ++y1Theta;
                }       
                //normalize theta�� (normalize�B�����ܬ�����, ����j�p��)
                x1Theta /= xOffset;
                z1Theta /= zOffset;
                y1Theta /= yOffset;
                //xz��V, �Ω������m
                int xDir = xOffset < 0.0D ? -1 : 1;
                int zDir = zOffset < 0.0D ? -1 : 1;
                int yDir = yOffset < 0.0D ? -1 : 1;
                //���opos2��Ʈy��
                int x2 = MathHelper.floor_double(pos2.xCoord);
                int z2 = MathHelper.floor_double(pos2.zCoord);
                int y2 = MathHelper.floor_double(pos2.yCoord);
                //�p��pos1,2��ƶZ��
                int xIntOffset = x2 - x1;
                int zIntOffset = z2 - z1;
                int yIntOffset = y2 - y1;
                
                //�Htheta�Ȱ����W, �ˬd�ӽu�W�g�L�|�I�쪺�Ҧ����, �O�_����w������, �Y���q�L����, �h�^��true
                do {
                	//�Y�T��V���S���Z���t, �h����
                    if(xIntOffset * xDir <= 0 && zIntOffset * zDir <= 0 && yIntOffset * yDir <= 0) {
//                    if(xIntOffset * xDir <= 0 && zIntOffset * zDir <= 0) {
                        return true;
                    }
                    
                    //��Xtheta�̤p���� (�̨S���Q���i�L����V), �Ӥ�V+1��
                    switch(FormatHelper.min(x1Theta, y1Theta, z1Theta)) {
                    case 1:
                    	x1Theta += xOffAbs;
                        x1 += xDir;
                        xIntOffset = x2 - x1;
                    	break;
                    case 2:
                    	y1Theta += yOffAbs;
                        y1 += yDir;
                        yIntOffset = y2 - y1;
                    	break;
                    case 3:
                    	z1Theta += zOffAbs;
                        z1 += zDir;
                        zIntOffset = z2 - z1;
                    	break;
                    default:
                    	break;
                    }
                }
                while(this.isSafeToStandAt(x1, y1, z1, xSize, ySize, zSize, pos1, xOffset, zOffset));
                //�L�k�q�L�ˬd, �^��false
                return false;
            }
        }
    }

    /**
     * Returns true when an entity could stand at a position, including solid blocks under the entire entity. Args:
     * xOffset, yOffset, zOffset, entityXSize, entityYSize, entityZSize, originPosition, vecX, vecZ
     */
    private boolean isSafeToStandAt(int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, Vec3 orgPos, double vecX, double vecZ) {
        int xSize2 = xOffset - xSize / 2;
        int zSize2 = zOffset - zSize / 2;
        
        //�|����entity�����ˬd���}�I
    	if(this.canFly) return true;
        
        //�Y�Ӧ�m������d��, �hfalse
        if(!this.isPositionClear(xSize2, yOffset, zSize2, xSize, ySize, zSize, orgPos, vecX, vecZ)) {
            return false;
        }
        else {
        	//���|����entity�����ˬd�������}���
            for(int x1 = xSize2; x1 < xSize2 + xSize; ++x1) {
                for(int z1 = zSize2; z1 < zSize2 + zSize; ++z1) {
                    double x2 = (double)x1 + 0.5D - orgPos.xCoord;
                    double z2 = (double)z1 + 0.5D - orgPos.zCoord;

                    //�ˬd���U����O�_�i�w������
                    if(x2 * vecX + z2 * vecZ >= 0.0D) {
                        Block block = this.worldObj.getBlock(x1, yOffset - 1, z1);
                        Material material = block.getMaterial();
                        //�Y���୸, ���U�S�Oair, �hfalse
                        if(block == null || material == Material.air) {
                            return false;
                        }
                    }
                }
            }

            return true;
        }
    }

    /**
     * Returns true if an entity does not collide with any solid blocks at the position. Args: xOffset, yOffset,
     * zOffset, entityXSize, entityYSize, entityZSize, originPosition, vecX, vecZ
     */
    private boolean isPositionClear(int xOffset, int yOffset, int zOffset, int xSize, int ySize, int zSize, Vec3 orgPos, double vecX, double vecZ) {
        for(int x1 = xOffset; x1 < xOffset + xSize; ++x1) {
            for(int y1 = yOffset; y1 < yOffset + ySize; ++y1) {
                for(int z1 = zOffset; z1 < zOffset + zSize; ++z1) {
                    double x2 = (double)x1 + 0.5D - orgPos.xCoord;
                    double z2 = (double)z1 + 0.5D - orgPos.zCoord;

                    if(x2 * vecX + z2 * vecZ >= 0.0D) {
                        Block block = this.worldObj.getBlock(x1, y1, z1);
                        
                        //�Y���w�������, �h����clear
                        if(EntityHelper.checkBlockSafe(block)) return true;
                        
                        if(block == Blocks.fence) {
                        	return false;
                        }
                        
                        //�Y�Ӥ������L���i�q�L���, �h��������ê��
                        if(!block.getBlocksMovement(this.worldObj, x1, y1, z1)) {
                        	return false;
                        }
                    }
                }
            }
        }

        return true;
    }
}
