package com.dd.dfs.ha;

import java.io.Serializable;

import org.apache.catalina.tribes.Member;



/**
 * cluster间发送的消息接口
 * @author DD
 *
 */
public interface ClusterMessage extends Serializable {
    public Member getAddress();
    public void setAddress(Member member);
    public String getUniqueId();
    public void setUniqueId(String id);
    public long getTimestamp();
    public void setTimestamp(long timestamp);
}
