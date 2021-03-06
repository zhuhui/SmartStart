package com.conghongjie.start.priority;


import android.content.Context;
import android.util.Log;

import com.conghongjie.start.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * 只负责计算优先级相关的工作
 * <p>
 * 节点的优先级 = 节点及节点后至少还需要多少时间
 *
 * @author conghongjie
 */
public class TaskPriorityManager {

    /**
     * 所有的节点，方便查找
     */
    HashMap<String, Task> allTasks = new HashMap<>();


    /**
     * 添加任务
     *
     * @param task
     */
    public void addTask(Context context, Task task) {
        synchronized (this) {
            // 非法检查
            if (allTasks.get(task.taskKey) != null) {
                throw new RuntimeException("taskKey has be used");
            }
            // 初始化任务的优先级信息
            task.priorityInfo = new TaskPriorityInfo();
            task.priorityInfo.priority = getPriority(context,task.taskKey);
            // 保存此任务
            allTasks.put(task.taskKey, task);
        }
    }


    /**
     * 获取最高优先级的任务
     */
    public Task getMaxPriorityTask(Collection<Task> tasks) {
        synchronized (this){
            JSONObject jsonObject = new JSONObject();
            Task maxPriorityTask = null;
            Iterator<Task> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                Task temp = iterator.next();
                try {
                    jsonObject.put(temp.taskKey,temp.priorityInfo.priority);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (maxPriorityTask == null || temp.priorityInfo.priority > maxPriorityTask.priorityInfo.priority) {
                    maxPriorityTask = temp;
                }
            }
            if (maxPriorityTask!=null){
                Log.e("StartStart"," -----选择"+maxPriorityTask.taskKey+"   "+jsonObject.toString());
            }
            return maxPriorityTask;
        }
    }

    /**
     * 计算并保存优先级
     */
    public void computeAndSavePriority(Context context) {
        // 先建立依赖关系网
        Iterator<Task> iterator = allTasks.values().iterator();
        while (iterator.hasNext()) {
            Task temp = iterator.next();
            for (int i = 0; i < temp.dependTasks.length; i++) {
                Task dependTask = allTasks.get(temp.dependTasks[i]);
                temp.priorityInfo.dependTasks.add(dependTask);
                dependTask.priorityInfo.dependedTasks.add(temp);
            }
        }
        // 计算所有节点后续还需要执行的总时间
        JSONObject jsonObject = new JSONObject();
        Iterator<Task> iterator2 = allTasks.values().iterator();
        while (iterator2.hasNext()) {
            Task temp = iterator2.next();
            computeTimeToEnd(temp);
            try {
                jsonObject.put(temp.taskKey, temp.priorityInfo.timeToEnd);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        // 保存优先级
        TaskPrioritySaver.savePriorities(context, jsonObject);
        Log.e("StartStart",jsonObject.toString());
    }


    /**
     * 释放资源
     */
    public void release() {
        allTasks.clear();
    }


    /**
     * 计算节点后还需要多久才能结束
     */
    private void computeTimeToEnd(Task task) {
        long maxDependedTimeToEnd = 0;
        for (int i = 0; i < task.priorityInfo.dependedTasks.size(); i++) {
            Task dependedTask = task.priorityInfo.dependedTasks.get(i);
            // 计算被依赖任务的总剩余时间
            if (dependedTask.priorityInfo.timeToEnd == -1) {
                computeTimeToEnd(dependedTask);
            }
            // 最大值
            long dependedTimeToEnd = dependedTask.priorityInfo.timeToEnd;
            if (dependedTimeToEnd > maxDependedTimeToEnd) {
                maxDependedTimeToEnd = dependedTimeToEnd;
            }
        }
        task.priorityInfo.timeToEnd = maxDependedTimeToEnd + task.priorityInfo.takeTime;
    }
    /**
     * 获取任务优先级
     *
     * @param context
     * @param taskKey
     * @return
     */
    JSONObject jsonObject = null;

    private long getPriority(Context context, String taskKey) {
        // 读取优先级缓存
        if (jsonObject == null) {
            jsonObject = TaskPrioritySaver.getPriorities(context);
        }
        // 设置优先级或者默认优先级
        try {
            return jsonObject.getLong(taskKey);
        } catch (JSONException e) {
            return 1;
        }
    }


    public void setTaskTakeTime(Task task, long takeTime) {
        task.priorityInfo.takeTime = takeTime;
    }
}
