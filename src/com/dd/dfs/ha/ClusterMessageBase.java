package com.dd.dfs.ha;

import org.apache.catalina.tribes.Member;


/**
 * 实现ClusterMessage的基础类
 * @author DD
 *
 */
public class ClusterMessageBase implements ClusterMessage {
    
    private static final long serialVersionUID = 1L;

    protected transient Member address;
    private String uniqueId;
    private long timestamp;
    public ClusterMessageBase() {
        // NO-OP
    }

    /**
     * getAddress
     *
     * @return Member
     * TODO Implement this org.apache.catalina.ha.ClusterMessage method
     */
    @Override
    public Member getAddress() {
        return address;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * setAddress
     *
     * @param member Member
     * TODO Implement this org.apache.catalina.ha.ClusterMessage method
     */
    @Override
    public void setAddress(Member member) {
        this.address = member;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
