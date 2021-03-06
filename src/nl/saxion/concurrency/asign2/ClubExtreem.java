package nl.saxion.concurrency.asign2;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClubExtreem {

    private final int maxUsers, consecutiveRepresentatives;

    ReentrantLock lock = new ReentrantLock();

    Condition representativeLimit = lock.newCondition();
    Condition representativeWaiting = lock.newCondition();
    Condition visitorWaiting = lock.newCondition();
    Condition representativeInClub = lock.newCondition();
    Condition clubNotHalfFull = lock.newCondition();

    private int currentVisitorsInClub = 0;

    private boolean isRepresentativeWaiting = false;
    private boolean isRepresentativeInClub = false;
    private int representativeCounter = 0;
    private int visitorsWaiting = 0;

    public ClubExtreem(int maxUsers, int consecutiveRepresentatives) {
        this.maxUsers = maxUsers;
        this.consecutiveRepresentatives = consecutiveRepresentatives;
    }

    private boolean isOverHalfCapacity(){
        return currentVisitorsInClub > (maxUsers/2);
    }

    private boolean isClubFull() {return currentVisitorsInClub >= maxUsers;}
    private boolean canVisitorEnter(){
        return !isClubFull() && !isRepresentativeInClub && (!isRepresentativeWaiting || representativeCounter == consecutiveRepresentatives);

    }

    public void joinClub(BaseVisitor visitor){
        //check if visitor is visitor or representative
        //call private joinClub with corresponding visitor type.
        if(visitor instanceof Visitor)
            joinClub((Visitor)visitor);
        else joinClub((Representative) visitor);
    }

    private void joinClub(Representative representative){
        //take lock
        lock.lock();
        try{
            // wachtrij -> in club? -> 3 op een rij? -> halfvol -> enter club -> leave club
            System.out.println("Representative " + representative.name + " wants to join the club");
            while(isRepresentativeWaiting)
                representativeWaiting.await();

            isRepresentativeWaiting = true;

            System.out.println("Representative " + representative.name + " waiting to enter club");
            while(isRepresentativeInClub)
                representativeInClub.await();

            //check if representativeCounter isn't equal to consecutiveRepresentatives
            System.out.println("Representative " + representative.name + " waiting till counter is < 3. counter = " + representativeCounter);
            while(representativeCounter >= consecutiveRepresentatives)
                representativeLimit.await();

            //check if club isn't over half capacity
            System.out.println("Representative " + representative.name + " waiting till capacity is < half");
            while(isOverHalfCapacity())
                clubNotHalfFull.await();

            //enter club
            currentVisitorsInClub++;
            representativeCounter++;
            isRepresentativeInClub = true;
            isRepresentativeWaiting = false;
            representativeWaiting.signal();
            System.out.println(representative.name + " entering club. There are now " + currentVisitorsInClub + "/" + maxUsers +" visitors in the club.");
        }
        //release lock
        catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }

    private void joinClub(Visitor visitor){
        //take lock
        lock.lock();
        try{
            while(representativeCounter >= consecutiveRepresentatives)
                representativeLimit.await();

            visitorsWaiting++;

//            System.out.println(visitor.name + " joining club, = "  + " visitors waiting = " + visitorsWaiting);
            while(!canVisitorEnter())
                visitorWaiting.await();

            currentVisitorsInClub++;
            visitorsWaiting--;
            if(visitorsWaiting <= 0){
                representativeCounter = 0;
                representativeLimit.signalAll();
            }
            System.out.println(visitor.name + " entering club. There are now " + currentVisitorsInClub + "/" + maxUsers + " visitors in the club." + "visitors waiting = " + visitorsWaiting);
        }
        //release lock
        catch (InterruptedException e) {
            e.printStackTrace();
        } finally{
            lock.unlock();
        }
    }

    public void leaveClub(BaseVisitor visitor){
        //take lock
        lock.lock();
        try{
            //check if basevisitor is representative or normal visitor
            if(visitor instanceof Representative){
                //if representative, put isRepresentativeInClub on false and lower visitor counter by 1
                //signal isRepresentativeInClub
                isRepresentativeInClub = false;
                representativeInClub.signal();
                currentVisitorsInClub--;
                visitorWaiting.signalAll();
                if(!isRepresentativeWaiting && visitorsWaiting > 0){
                    representativeCounter = 0;
                    representativeLimit.signal();
                }
                System.out.println(visitor.name + " leaving club. There are now " + currentVisitorsInClub  + "/" + maxUsers + " visitors in the club.");
            }else{
                //if visitor, lower visitor counter by 1
                //signal clubIsNotHalfFull
                currentVisitorsInClub--;
                clubNotHalfFull.signal();
                visitorWaiting.signalAll();
                System.out.println(visitor.name + " leaving club. There are now " + currentVisitorsInClub  + "/" + maxUsers + " visitors in the club.");
            }
        }
        //release lock
        finally {
            lock.unlock();
        }
    }



//    public void joinClub(Queue<Representative> waitingRepresentatives, Queue<Visitor> waitingVisitors){
//        lock.lock();
//        try {
//            //if there are no waiting visitors wait
//            while (waitingRepresentatives.size() == 0 && waitingVisitors.size() == 0) {
//                emptyQueues.await();
//            }
//            //if there are representatives join the club
//            if (waitingRepresentatives.size() != 0 && representativeCounter != 3) {
//                while (currentVisitorsInClub == 10) {
//                    clubIsFull.await();
//                }
//                representativeCounter++;
//                waitingRepresentatives.remove();
//            //if there are regular visitors join the club
//            } else {
//                while (currentVisitorsInClub == 20) {
//                    clubIsFull.await();
//                }
//                currentVisitorsInClub++;
//                representativeCounter =0;
//                waitingVisitors.remove();
//            }
//        }catch(Exception e){
//            System.out.println(e);
//        }finally {
//            lock.unlock();
//        }
//    }
//
//    public void leaveClub(BaseVisitor visitor){
//        lock.lock();
//
//        try{
//            while (visitor.timeInClub){
//                //wait
//            }
//            if(visitor.timeInClub == 0){
//                currentUsersInClub--;
//                clubIsFull.signal();
//            }
//        }catch(Exception e){
//            System.out.println(e);
//        }finally {
//            lock.unlock();
//        }
//    };
//

}
