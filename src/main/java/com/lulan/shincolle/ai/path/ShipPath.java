package com.lulan.shincolle.ai.path;

/**SHIP PATH
 * �s��Ҧ�path point, �åB�̷Ӳ��I�Z�����Ƨ�
 * point�bpath array�������Ǹ�point�b��ڸ��|�W������(index)�L��, ��¬O�Z���j�p�Ƨ�
 */
public class ShipPath {
    /** Contains the points in this path */
    private ShipPathPoint[] pathPoints = new ShipPathPoint[1024];
    /** The number of points in this path */
    private int count;


    public ShipPathPoint[] getPathPoints() {
    	return pathPoints;
    }
    
    public int getCount() {
    	return count;
    }
    
    /**
     * Adds a point to the path
     */
    public ShipPathPoint addPoint(ShipPathPoint point) {
        if(point.index >= 0) {
            throw new IllegalStateException("OW KNOWS!");
        }
        else {
            if(this.count == this.pathPoints.length) {
            	ShipPathPoint[] apathpoint = new ShipPathPoint[this.count << 1];
                System.arraycopy(this.pathPoints, 0, apathpoint, 0, this.count);
                this.pathPoints = apathpoint;
            }

            this.pathPoints[this.count] = point;
            point.index = this.count;
            this.sortBack(this.count++);
            return point;
        }
    }

    /**
     * Clears the path
     */
    public void clearPath() {
        this.count = 0;
    }

    /**
     * Returns and removes the first point in the path
     */
    public ShipPathPoint dequeue() {
    	ShipPathPoint pathpoint = this.pathPoints[0];		//�Ȧs�U�Ĥ@���I
        this.pathPoints[0] = this.pathPoints[--this.count];	//�N�̫�@���I�s��a�@���I��m
        this.pathPoints[this.count] = null;					//�̫�@���I+1����m�]��null

        if(this.count > 0) {	//�N�Ĥ@���I���k���@���Ƨ�
            this.sortForward(0);
        }

        pathpoint.index = -1;	//���X���Iid�]��-1
        return pathpoint;
    }

    /**
     * Changes the provided point's distance to target
     */
    public void changeDistance(ShipPathPoint point, float dist)
    {
        float f1 = point.distanceToTarget;
        point.distanceToTarget = dist;

        if(dist < f1) {
            this.sortBack(point.index);
        }
        else {
            this.sortForward(point.index);
        }
    }

    /**
     * Sorts a point to the left
     */
    private void sortBack(int id) {
        ShipPathPoint pathpoint = this.pathPoints[id];
        int j;

        //�ثe���I��e���������I������I�Z��, ����I����I�Z����j�̰��� (���I�Z���j = ���ؼл����I)
        for(float f = pathpoint.distanceToTarget; id > 0; id = j) {
            j = id - 1 >> 1;
            ShipPathPoint pathpoint1 = this.pathPoints[j];

            if(f >= pathpoint1.distanceToTarget) {
                break;
            }

            this.pathPoints[id] = pathpoint1;
            pathpoint1.index = id;
        }

        this.pathPoints[id] = pathpoint;
        pathpoint.index = id;
    }

    /**
     * Sorts a point to the right
     */
    private void sortForward(int id) {
    	ShipPathPoint pathpoint = this.pathPoints[id];
        float f = pathpoint.distanceToTarget;

        while(true) {
            int j = 1 + (id << 1);
            int k = j + 1;

            if(j >= this.count) {
                break;
            }

            ShipPathPoint pathpoint1 = this.pathPoints[j];
            float f1 = pathpoint1.distanceToTarget;
            ShipPathPoint pathpoint2;
            float f2;

            if(k >= this.count) {
                pathpoint2 = null;
                f2 = Float.POSITIVE_INFINITY;
            }
            else {
                pathpoint2 = this.pathPoints[k];
                f2 = pathpoint2.distanceToTarget;
            }

            if(f1 < f2) {
                if(f1 >= f) {
                    break;
                }

                this.pathPoints[id] = pathpoint1;
                pathpoint1.index = id;
                id = j;
            }
            else {
                if(f2 >= f) {
                    break;
                }

                this.pathPoints[id] = pathpoint2;
                pathpoint2.index = id;
                id = k;
            }
        }

        this.pathPoints[id] = pathpoint;
        pathpoint.index = id;
    }

    /**
     * Returns true if this path contains no points
     */
    public boolean isPathEmpty() {
        return this.count == 0;
    }
}
