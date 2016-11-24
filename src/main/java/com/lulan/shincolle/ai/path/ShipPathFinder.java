package com.lulan.shincolle.ai.path;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import com.lulan.shincolle.reference.Enums.EnumPathType;
import com.lulan.shincolle.utility.BlockHelper;

/**SHIP PATH FINDER
 * for ship path navigator
 * 建立空中 or 水中path, 無視重力跟浮力
 */
public class ShipPathFinder
{
    /** Used to find obstacles */
    private IBlockAccess world;
    /** The path being generated */
    private ShipPathHeap path = new ShipPathHeap();
    /** The points in the path */
    private IntHashMap pointMap = new IntHashMap();
    /** is air path, for non-airplane entity */
    private boolean isPathingInAir;
    /** is airplane */
    private boolean canEntityFly;


    public ShipPathFinder(IBlockAccess world, boolean canFly)
    {
        this.world = world;
        this.isPathingInAir = false;
        this.canEntityFly = canFly;
    }

    /**
     * Creates a path from one entity to another within a minimum distance
     */
    @Nullable
    public ShipPath findPath(Entity fromEnt, Entity toEnt, float range)
    {
        return this.findPath(fromEnt, toEnt.posX, toEnt.getEntityBoundingBox().minY, toEnt.posZ, range);
    }

    /**
     * Creates a path from an entity to a specified location within a minimum distance
     */
    @Nullable
    public ShipPath findPath(Entity entity, int x, int y, int z, float range)
    {
        return this.findPath(entity, x + 0.5F, y + 0.5F, z + 0.5F, range);
    }

    /**
     * Internal implementation of creating a path from an entity to a point
     */
    @Nullable
    private ShipPath findPath(Entity entity, double x, double y, double z, float range)
    {
        this.path.clearPath();
        this.pointMap.clearMap();
        int i;

    	i = MathHelper.floor_double(entity.getEntityBoundingBox().minY + 0.5D);

        //將起點終點加入point map
    	//設定起點終點: 將entity位置(double)轉為整數位置(int)
        ShipPathPoint startpp = this.openPoint(MathHelper.floor_double(entity.getEntityBoundingBox().minX), i, MathHelper.floor_double(entity.getEntityBoundingBox().minZ));
        ShipPathPoint endpp = this.openPoint(MathHelper.floor_double(x - entity.width * 0.5F), MathHelper.floor_double(y), MathHelper.floor_double(z - entity.width * 0.5F));
        
        //目標的長寬高+1建立為一個path point, 用於判定路徑寬高是否會卡住該entity
        ShipPathPoint entitySize = new ShipPathPoint(MathHelper.floor_float(entity.width + 1F), MathHelper.floor_float(entity.height + 1F), MathHelper.floor_float(entity.width + 1F));
        
        //計算出起點終點之間所有點
        ShipPath pathentity = this.findPath(entity, startpp, endpp, entitySize, range);
        
        return pathentity;
    }

    /**
     * Adds a path from start to end and returns the whole path (args: entity, start, end, unused, maxDistance)
     *
     * 1.9.4:
     * 找點的計算方式改用曼哈頓距離來判定, 較適合於方塊世界中的路徑尋找
     * 使路徑選項可能性變多, 而不是堅持走最短的直線距離
     * 
     * ex: 在曼哈頓距離中, 右2+上2 = 上1右1上1右1, 兩種皆可使用
     *     如果用歐式距離, 右2+上2 > 上1右1上1右1, 只會選後者為路徑唯一選項
     * 
     */
    @Nullable
    private ShipPath findPath(Entity entity, ShipPathPoint startpp, ShipPathPoint endpp, ShipPathPoint entitySize, float range)
    {
        startpp.totalPathDistance = 0F;
        startpp.distanceToNext = startpp.distanceManhattan(endpp);
        startpp.distanceToTarget = startpp.distanceToNext;
        this.path.clearPath();
        this.path.addPoint(startpp);
        ShipPathPoint ppTemp = startpp;
        int findCount = 0;

        /**path建立方法
         * 從起點開始, 找出可移動方向, 將判定距離目標最短的點加入path
         */
        while (!this.path.isPathEmpty())
        {
        	//從path中取出一點
        	ShipPathPoint ppDequeue = this.path.dequeue();
        	findCount++;
        	
        	//若取出點=終點, 則完成path
            if (ppDequeue.equals(endpp))
            {
            	//將所有point做成path entity
//            	LogHelper.info("DEBUG : path navi: find count (pathing done) "+findCount);
                return this.createEntityPath(startpp, endpp);
            }
            
            //若while跑太多次, 強制中止
            if (findCount > 450)
            {
            	break;
            }
            
            //若取出點的終點距離較temp點小 (更接近終點), 將取出點定為temp點
            if (ppDequeue.distanceToSquared(endpp) < ppTemp.distanceToSquared(endpp))
            {
                ppTemp = ppDequeue;
            }
            
            //將取出點標記為已拜訪過, 接著尋找該點周圍哪些點可走
            ppDequeue.visited = true;
            ShipPathPoint[] findOption = this.findPathOptions(entity, ppDequeue, entitySize, endpp, range); //取得可走方向
            
            //對所有可走方向, 計算曼哈頓距離, 取最短且沒走過的點加入path
            for (ShipPathPoint pp : findOption)
            {
            	//取出可走點, 計算曼哈頓距離
                float dist = ppDequeue.distanceManhattan(pp);
                
                //設定可走點的距離成本
                pp.distanceFromOrigin = ppDequeue.distanceFromOrigin + dist;
                pp.cost = dist + pp.costMalus;
                float dist2 = ppDequeue.totalPathDistance + pp.cost;

                //若可走點距離尚未超出路徑上限距離, 且沒走過, 則可加入到path中
                if (pp.distanceFromOrigin < range && (!pp.isAssigned() || dist2 < pp.totalPathDistance))
                {
                	pp.previous = ppDequeue;
                	pp.totalPathDistance = dist2;
                	pp.distanceToNext = pp.distanceManhattan(endpp) + pp.costMalus;

                	//若這個點已經存在於path中, 則計算新距離並重新排序該點在path中的位置
                    if (pp.isAssigned())
                    {
                        this.path.changeDistance(pp, pp.totalPathDistance + pp.distanceToNext);
                    }
                    //若該點不在path中, 則加入到path
                    else
                    {
                    	pp.distanceToTarget = pp.totalPathDistance + pp.distanceToNext;
                        this.path.addPoint(pp);
                    }
                }
            }
            
            //1.7.10 clean mark
            //將所有可走方向都嘗試走看看
//            for (int j = 0; j < i; ++j)
//            {
//            	ShipPathPoint ppTemp2 = this.pathOptions[j];
//                float f1 = ppDequeue.totalPathDistance + ppDequeue.distanceToSquared(ppTemp2);
//                
//                //若該點還沒加入過path, 或該點已加入到path中但該點之前算出來的path長度較長, 則需要再更新一次path長度
//                if(!ppTemp2.isAssigned() || f1 < ppTemp2.totalPathDistance) {
//                	//將該點正式加入到path中, 設定其前後點跟path長度值
//                    ppTemp2.previous = ppDequeue;		//前一點設為取出點
//                    ppTemp2.totalPathDistance = f1;		//存下其path長度值
//                    ppTemp2.distanceToNext = ppTemp2.distanceToSquared(endpp);	//下一點設為終點
//                    //若該點本來就在path中, 則更新其path長度值
//                    if(ppTemp2.isAssigned()) {
//                        this.path.changeDistance(ppTemp2, ppTemp2.totalPathDistance + ppTemp2.distanceToNext);
//                    }
//                    else {	//不在path中, 正式加入path
//                        ppTemp2.distanceToTarget = ppTemp2.totalPathDistance + ppTemp2.distanceToNext;
//                        this.path.addPoint(ppTemp2);
//                    }
//                }
//            }
            
        }//end while
        
        //若完全找不到點可以加入, 則回傳null, 表示找不到path
        if(ppTemp == startpp)
        {
            return null;
        }
        else
        {
        	//將所有point做成path entity
//        	LogHelper.info("DEBUG : find path: fail: find count = "+findCount+" times");
            return this.createEntityPath(startpp, ppTemp);
        }
    }

    /**判定此點到終點的path有幾個方向可走
     * populates pathOptions with available points and returns the number of options found (args: unused1, currentPoint,
     * unused2, targetPoint, maxDistance)
     * 
     * pathCase: 1: can go UP
     */
    private ShipPathPoint[] findPathOptions(Entity entity, ShipPathPoint currentpp, ShipPathPoint entitySize, ShipPathPoint targetpp, float range)
    {
        //若頭頂沒卡到東西, 則pathCase = 1, 表示可往上1格找路徑
        EnumPathType type = getPathType(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize);
        int pathYOffset = 0;
        
        if (type == EnumPathType.FLUID || (type == EnumPathType.OPEN && this.canEntityFly))
        {
        	pathYOffset = 1;
        }
        
        //檢查東西南北上下 + 四個水平對角方向
        //array: Down Up Northwest Northeast Southwest Southeast North South East West
        ShipPathPoint[] pp = new ShipPathPoint[10];
        pp[0] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord - 1, currentpp.zCoord, entitySize, pathYOffset);
        pp[1] = null;
        pp[2] = null;
        pp[3] = null;
        pp[4] = null;
        pp[5] = null;
        pp[6] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord - 1, entitySize, pathYOffset);
        pp[7] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord, currentpp.zCoord + 1, entitySize, pathYOffset);
        pp[8] = this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord, entitySize, pathYOffset);
        pp[9] = this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord, entitySize, pathYOffset);

        boolean fn = pp[6] == null || pp[6].costMalus != 0F;	//NYI: cost malus calc in getSafePoint
        boolean fs = pp[7] == null || pp[7].costMalus != 0F;
        boolean fe = pp[8] == null || pp[8].costMalus != 0F;
        boolean fw = pp[9] == null || pp[9].costMalus != 0F;
        
        //上面為液體方塊才允許往上找路徑, 若上面為空氣, 則只有可以飛的才往上找路徑
        if (pathYOffset > 0)
        {
        	pp[1] = this.getSafePoint(entity, currentpp.xCoord, currentpp.yCoord + 1, currentpp.zCoord, entitySize, pathYOffset);
        }
        
        if (fn && fw)  //northwest
        {
        	pp[2] = this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord - 1, entitySize, pathYOffset);
        }
        
        if (fn && fe)  //northeast
        {
        	pp[3] = this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord - 1, entitySize, pathYOffset);
        }
        
        if (fs && fw)  //southwest
        {
        	pp[4] = this.getSafePoint(entity, currentpp.xCoord - 1, currentpp.yCoord, currentpp.zCoord + 1, entitySize, pathYOffset);
        }
        
        if (fs && fe)  //southeast
        {
        	pp[5] = this.getSafePoint(entity, currentpp.xCoord + 1, currentpp.yCoord, currentpp.zCoord + 1, entitySize, pathYOffset);
        }
        
        //若該點為安全點 & 沒有拜訪過 & 在尋路最大範圍內, 則加入到可拜訪選項中
        List<ShipPathPoint> temp = new ArrayList<ShipPathPoint>();
        int len = 0;
        
        for (ShipPathPoint spp : pp)
        {
            if(spp != null && !spp.visited && spp.distanceTo(targetpp) < range)
            {
            	temp.add(spp);
            	len++;
            }
        }
        
        //list to array
        ShipPathPoint[] ret = temp.toArray(new ShipPathPoint[len]);
        
        return ret;
    }

    /**找指定點(x,y,z)是否可安全移動過去, 包括該點下面是否可安全站立 (change: 修改水跟岩漿也算可站立方塊)
     * Returns a point that the entity can safely move to
     * pathOption:  0:blocked  1:clear
     */
    private ShipPathPoint getSafePoint(Entity entity, int x, int y, int z, ShipPathPoint entitySize, int pathYOffset)
    {
    	ShipPathPoint pathpoint1 = null;
    	EnumPathType pathCase = getPathType(entity, x, y, z, entitySize);	//取得該點路況
        
        //若該點路況為: open trapdoor or 液體, 則接受為safe point
        if (pathCase == EnumPathType.FLUID || pathCase == EnumPathType.DOOR_CLOSE)
        {
            return this.openPoint(x, y, z);
        }
        //其他路況
        else
        {
        	//若路況為clear, 則把該點加入到path
            if (pathCase == EnumPathType.OPEN)
            {
                pathpoint1 = this.openPoint(x, y, z);
            }
            
            //若路況是實體方塊, 且允許往上找路徑, 則往上找
            if (pathpoint1 == null && pathYOffset > 0 &&
            	getPathType(entity, x, y + pathYOffset, z, entitySize) == EnumPathType.OPEN)
            {
                pathpoint1 = this.openPoint(x, y + pathYOffset, z);	//把往上一點加入到path
                y += pathYOffset;
            }
            
            //若有找到可加入path的點, 則往下找該點底下方塊是否可安全站立(只往下找X格, X依照entity自己的getMaxSafePointTries決定)
            if (pathpoint1 != null)
            {
            	//若可以飛, 則直接回傳pathpoint
            	if (this.canEntityFly)
            	{
            		return pathpoint1;
            	}
            	
            	//若不能飛, 則往下找安全落地點
                int j1 = 0;
                
                while (y > 0)
                {
                	EnumPathType downCase = getPathType(entity, x, y - 1, z, entitySize);
                    //若底下全都為液體(本體在空中, 因此會掉到液體方塊中), 則path點高度改為在液體中
                    if (downCase == EnumPathType.FLUID)
                    {
                    	pathpoint1 = this.openPoint(x, y - 1, z);
                        break;
                    }
                    
                    //若無法往下, 表示已經落地, 則以此點作為path點
                    if (downCase != EnumPathType.OPEN)
                    {
                        break;
                    }
                    
                    //若嘗試超過特定次數, 則判定沒有安全落地, 傳回null
                    if (j1++ > 64)
                    {
                        return null;
                    }
                    
                    //往下找落地點, 將落地點加入到path
                    --y;
                    
                    if (y > 0)
                    {
                        pathpoint1 = this.openPoint(x, y, z);
                    }
                }//end fall while
            }
            
            return pathpoint1;
        }
    }

    /**
     * Returns a mapped point or creates and adds one
     */
    private final ShipPathPoint openPoint(int x, int y, int z)
    {
        int l = ShipPathPoint.makeHash(x, y, z);	//座標值算hash
        ShipPathPoint pathpoint = (ShipPathPoint) this.pointMap.lookup(l);	//用hash值找路徑中是否有該點

        //path中找不到該點, 則建立之
        if (pathpoint == null)
        {
            pathpoint = new ShipPathPoint(x, y, z);
            this.pointMap.addKey(l, pathpoint);
        }

        return pathpoint;
    }

    /**
     * 1.9.4:
     * changed return type to EnumPathType
     * EnumPathType = OPEN, FLUID, BLOCKED, DOOR_CLOSE...
     * 
     * 1.7.10:
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
    public EnumPathType getPathType(Entity entity, int x, int y, int z, ShipPathPoint entitySize)
    {
        return getPathType(entity, x, y, z, entitySize, this.isPathingInAir);
    }

    public static EnumPathType getPathType(Entity entity, int x, int y, int z, ShipPathPoint entitySize, boolean inAir)
    {
        boolean pathToDoor = false;		//是否有打開陷阱門AI
        boolean pathInLiquid = true;	//是否是站在液體中的path
        boolean blocked = false;
        
        //判定該point加上entity的長寬高後, 是否會碰撞到其他方塊
        for (int x1 = x; x1 < x + entitySize.xCoord; ++x1)
        {
            for (int y1 = y; y1 < y + entitySize.yCoord; ++y1)
            {
                for (int z1 = z; z1 < z + entitySize.zCoord; ++z1)
                {
                	//get block
                	IBlockState block = entity.worldObj.getBlockState(new BlockPos(x1, y1, z1));
                	Material mat = null;
                	
                	if (block != null) mat = block.getMaterial();
                	
                	//若碰到非空氣方塊
                	if (mat != null && mat != Material.AIR)
                	{
                        //若碰到不能穿過的方塊
                        if (mat.blocksMovement())
                        {
                            //is open door or openable fence gate
                            if (BlockHelper.isOpenableDoor(block))
                            {
                            	pathToDoor = true;
                            }
                            else
                            {
                            	//其他不能通過的方塊, 皆為blocked
                            	return EnumPathType.BLOCKED;
                            }
                        }
                        
                		//檢查原本高度y是否全為液體
                        if (pathInLiquid && y1 == y && !BlockHelper.checkBlockIsLiquid(block))
                        {
                        	pathInLiquid = false;
                        }
                	}
                	//is AIR block
                	else
                	{
                		//若為air方塊, 檢查高度y是否為液體
                        if (pathInLiquid && y1 == y)
                        {
                        	pathInLiquid = false;
                        }
                	}//end is air block
                }//end for z
            }//end for y
        }//end for x

        return pathInLiquid ? EnumPathType.FLUID :
        	   pathToDoor ? EnumPathType.DOOR_CLOSE : EnumPathType.OPEN;
    }

    /**
     * Returns a new PathEntity for a given start and end point
     */
    private ShipPath createEntityPath(ShipPathPoint startpp, ShipPathPoint endpp)
    {
        int i = 1;
        ShipPathPoint pathpoint2;

        //找出path總點數, 存為i
        for (pathpoint2 = endpp; pathpoint2.previous != null; pathpoint2 = pathpoint2.previous)
        {
            ++i;
        }

        ShipPathPoint[] pathtemp = new ShipPathPoint[i];
        pathpoint2 = endpp;
        --i;

        //將path所有點存到pathtemp
        for (pathtemp[i] = endpp; pathpoint2.previous != null; pathtemp[i] = pathpoint2)
        {
            pathpoint2 = pathpoint2.previous;
            --i;
        }

        return new ShipPath(pathtemp);
    }
    
    
}
