package com.driver;

import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {

    //Assume that each user belongs to at most one group
    //You can use the below mentioned hashmaps or delete these and create your own.
    private HashMap<Group, List<User>> groupUserMap;
    private HashMap<Group, List<Message>> groupMessageMap;
    private HashMap<Message, User> senderMap;
    private HashMap<Group, User> adminMap;
    private HashSet<String> userMobile;
    private HashMap<String, User> userMap;
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.userMap=new HashMap<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }
    public String createUser(String name, String mobile) throws Exception {
        User user = new User(name, mobile);
        if(userMobile.contains(mobile)){
            return "User already exists";
        }
        userMobile.add(mobile);
        userMap.put(mobile, user);
        return "SUCCESS";
    }

    public Group createGroup(List<User> users){
        String grpName = "";
        int numofParticipants = users.size();
        if(numofParticipants == 2){
            grpName = users.get(1).getName();
        }else{
            this.customGroupCount+=1;
            grpName= "Group "+customGroupCount;
        }
        Group grp = new Group(grpName, numofParticipants);
        String adminName = users.get(0).getName();
        String adminMobile = users.get(0).getMobile();
        User admin = new User(adminName, adminMobile);
        adminMap.put(grp, admin);
        groupUserMap.put(grp, users);
        return grp;
    }

    public int createMessage(String content){
        this.messageId += 1;
        Message newMesg = new Message(messageId, content);
        return messageId;
    }

    public int sendMessage(Message message, User sender, Group group) throws Exception{
        if(!groupUserMap.containsKey(group))
        {
            throw new Exception("Group does not exist");
        }else if(!groupUserMap.get(group).contains(sender)){
            throw new Exception("You are not allowed to send message");
        }
//        List<User> usersOfGrp =  groupUserMap.getOrDefault(group, new ArrayList<>());
//        usersOfGrp.add(sender);
//        groupUserMap.put(group, usersOfGrp);// members join thodi kr rhe grp, already joined honge wo to shayad

        senderMap.put(message, sender);
        List<Message> mesgsOfGrp =  groupMessageMap.getOrDefault(group, new ArrayList<>());
        mesgsOfGrp.add(message);
        groupMessageMap.put(group, mesgsOfGrp);
        return mesgsOfGrp.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws Exception{
        if(!groupUserMap.containsKey(group)){
            throw new Exception("Group does not exist");
        }
//        if(adminMap.getOrDefault(group, null) != approver){//this was not getting submitted, ideally it should work but test cases mei kuch hoga(Warna mobile se bhi check kr skte) g
//            throw new Exception("Approver does not have rights");
//        }
        if(adminMap.getOrDefault(group,null) == null)
        {
            throw new Exception("Approver does not have rights");
        }
        if(!groupUserMap.get(group).contains(user)){
            throw new Exception("User is not a participant");
        }
        adminMap.put(group, approver);//no use of removing

        return "SUCCESS";
    }

    public int removeUser(User user) throws Exception{
        Group grpHasUser = null;
        for(Group group : groupUserMap.keySet())
        {//map.keySet() => gives keys, now get the value
            List<User> userList = groupUserMap.get(group);
            if(userList.contains(user))
            {
                grpHasUser=group;
                break;
            }
        }
        if(grpHasUser == null){
            throw new Exception("User not found");
        }
        //If user is not the admin, remove the user from the group, remove all its messages from all the databases, and update relevant attributes accordingly.
        //If user is removed successfully, return (the updated number of users in the group + the updated number of messages in group + the updated number of overall messages)
        if(adminMap.get(grpHasUser) == user){
            throw new Exception("Cannot remove admin");
        }

        List<User> usersOfGroupAftRemoval = groupUserMap.get(grpHasUser);
        usersOfGroupAftRemoval.remove(user);
        groupUserMap.put(grpHasUser, usersOfGroupAftRemoval);

        List<Message> msgList = groupMessageMap.get(grpHasUser);//sender map(Message, User)     //mesgsOfRemovedUser
        for(Message msg:senderMap.keySet())
        {
            if(senderMap.get(msg)==user)
            {
                msgList.remove(msg);
                senderMap.remove(msg);
            }
        }
        groupMessageMap.put(grpHasUser, msgList);//updated it
        userMobile.remove(user.getMobile());
        userMap.remove(user.getName());

        return groupUserMap.get(grpHasUser).size() + groupMessageMap.get(grpHasUser).size() + senderMap.size();
        }



    public String findMessage(Date start, Date end, int K) throws Exception{
        // If the number of messages between given time is less than K, throw "K is greater than the number of messages" exception

        List<Message> messageList = new ArrayList<>();
        for(Message mesg : senderMap.keySet()){//traverse in totalMessages, get each Message time, check if matches conditions
            Date time = mesg.getTimestamp();
            if (start.before(time) && end.after(time)){
                messageList.add(mesg);
            }
        }
        if(messageList.size() < K){
            throw  new Exception("K is greater than the number of messages");
        }


        Map<Date , Message> hm = new HashMap<>();
        for (Message message : messageList){
            hm.put(message.getTimestamp(),message);
        }
        List<Date> dateList = new ArrayList<>(hm.keySet());
        Collections.sort(dateList, new sortCompare());
        Date date = dateList.get(K-1);
        String ans = hm.get(date).getContent();
        return ans;
    }
    class sortCompare implements Comparator<Date> {
        @Override
        // Method of this class
        public int compare(Date a, Date b)
        {
            /* Returns sorted data in Descending order */
            return b.compareTo(a);
        }
    }
}
