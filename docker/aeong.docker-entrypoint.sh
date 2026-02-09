#!/bin/bash

# start aeong server
start() {
   /home/AeonG/build/memgraph --bolt-port 7687 \
   --data-directory /database/      \
   --log-file=/database/aeong.log  \
   --log-level=DEBUG           \
   --also-log-to-stderr        \
   --bolt-server-name-for-init=Neo4j/5.2.0 \
   --storage-snapshot-on-exit=true         \
   --storage-recover-on-startup=true       \
   --storage-snapshot-interval-sec 30      \
   --storage-gc-cycle-sec 30   \
   --storage-properties-on-edges=true     \
   --memory-limit 0            \
   --anchor-num 10             \
   --real-time-flag=false
}


clean_lock_file(){
   local lock_dir="/database"
   echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 开始检查并清理$lock_dir下的LOCK文件..."
    if [ -d "$lock_dir" ]; then
        # 查找所有目录下的LOCK文件（仅找文件，排除目录）
        lock_files=$(find "$lock_dir" -name "LOCK" -type f)
        
        if [ -n "$lock_files" ]; then
            # 遍历并删除每个LOCK文件
            for lock_file in $lock_files; do
                echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 删除锁文件：$lock_file"
                rm -f "$lock_file" 2>/dev/null
                # 检查删除是否成功
                if [ -f "$lock_file" ]; then
                    echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 警告：无法删除锁文件 $lock_file（权限不足或文件不存在）"
                fi
            done
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] LOCK文件清理完成"
        else
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] $lock_dir下未找到任何LOCK文件"
        fi
    else
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 警告：目录 $lock_dir 不存在，跳过LOCK文件清理"
    fi
    find "$lock_dir"
    return 0
}

# 安全退出memgraph进程的函数
stop() {
    # 定义重试次数和等待时间
    local max_interrupt_attempts=4
    local wait_seconds=5
    local attempt=0
    local memgraph_pid

    # 循环发送INTERRUPT信号，直到达到最大重试次数
    while [ $attempt -lt $max_interrupt_attempts ]; do
        # 查找memgraph相关进程（排除grep自身），获取PID
        memgraph_pid=$(ps aux | grep -v grep | grep -i memgraph | awk '{print $2}')
        
        if [ -z "$memgraph_pid" ]; then
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 未检测到memgraph进程，退出循环"
            return 0
        fi

        # 发送INTERRUPT信号（等同于SIGINT，信号2）
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 第 $((attempt+1)) 次尝试：向memgraph进程(PID: $memgraph_pid)发送INTERRUPT信号"
        kill -INT "$memgraph_pid" 2>/dev/null

        # 等待指定时间后再次检查
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 等待 $wait_seconds 秒后检查进程状态..."
        sleep $wait_seconds

        # 检查进程是否已退出
        if ! ps -p "$memgraph_pid" >/dev/null 2>&1; then
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] memgraph进程(PID: $memgraph_pid)已退出"
            clean_lock_file
            return 0
        fi

        attempt=$((attempt + 1))
    done

    # 如果重试4次INTERRUPT后进程仍存在，发送TERM信号（信号15）
    memgraph_pid=$(ps aux | grep -v grep | grep -i memgraph | awk '{print $2}')
    if [ -n "$memgraph_pid" ]; then
        echo "[$(date +%Y-%m-%d\ %H:%M:%S)] INTERRUPT信号重试$max_interrupt_attempts次无效，发送TERM信号"
        kill -TERM "$memgraph_pid" 2>/dev/null
        
        # 最后检查一次进程状态
        sleep 2
        if ps -p "$memgraph_pid" >/dev/null 2>&1; then
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] 警告：TERM信号发送后memgraph进程仍未退出"
            clean_lock_file
            return 1
        else
            echo "[$(date +%Y-%m-%d\ %H:%M:%S)] memgraph进程已通过TERM信号终止"
            clean_lock_file
            return 0
        fi
    fi
    clean_lock_file
    return 0
}


# 处理命令行参数
main() {
    # 如果没有参数，执行默认函数
    if [ $# -eq 0 ]; then
        start_aeong_server
    else
        # 遍历所有参数并执行对应的函数
        for arg in "$@"; do
            if [ "$(type -t "$arg")" = "function" ]; then
                $arg  # 调用对应函数
            else
                echo "Warning: Bash function '$arg' undefined, use args as cmd."
                $arg
#                exit 1
            fi
        done
    fi
}

# 调用主函数并传递所有参数
main "$@"
