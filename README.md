# TGraph demo test
> TGraph is a database system designed for managing temporal graph data. 

Test cases with demo data of TGraph Database

# TGraph时态图数据库Demo测试

本项目通过三个典型的属性演化型时态图数据集构建了三个典型的基于时态图数据的应用系统，
并选取其中典型查询测试TGraph时态图数据库系统在特定应用场景下的的性能（并与Neo4j、MariaDB、PostgreSQL对比）。

| 数据集名称 | 数据集内容 | 应用场景 |
| ----  | ------ | ----- |
| BJ Traffic | 北京市道路交通数据 | 道路交通信息管理系统 |
| EU Energy |欧洲可再生能源电力系统数据 | 电力网络监控调度系统 |
| SYN | 合成数据 | 无 |

项目具体包括以下几个功能（main目录下代码）：
0. 下载BJ Traffic数据集
1. 对BJ Traffic和EU Energy数据集进行统计分析
2. 指定数据集、相关参数，根据参数生成对应应用场景下典型查询的benchmark（queries）
3. 指定数据集、benchmark，生成benchmark中查询的结果集（带结果集的benchmark）用于校验正确性
4. 指定数据集、benchmark、运行系统，在指定系统上运行该benchmark并收集metrics
5. 指定数据集、带结果集的benchmark、运行系统，在指定系统上运行该benchmark，收集结果并校验正确性
6. 根据收集的metrics生成性能测试报告
-----
单个测试（test目录下的代码）：
1. 按时间递增写入时态数据
2. Snapshot查询测试
3. SnapshotAggrMax查询测试
4. SnapshotAggrDuration查询测试
5. EntityTemporalCondition查询
6. 修改历史数据测试
7. 读写并发测试

下面我们具体说明使用方法

## 使用方法
系统大部分参数通过环境变量指定
```shell script
#公共参数说明

```
### 数据统计

### 生成benchmark
```shell
# BJ Traffic
source runTask.sh
genBenchmark BJTraffic 
```
### 生成benchmark的结果集

### 校验benchmark正确性

### 运行benchmark

### 生成报告

# 架构
每个查询被封装成一个事务来实现。每个事务都有一些参数，查询的结果集，以及执行时生成的metrics如执行时间，是否失败。

有几个主要模块：
- transaction 所有时态数据集的通用查询和部分数据集的特殊查询的事务。
- client 所有系统client端的实现，用于发起对于server的请求
- model 每个数据集的内存模型，结果集的生成通过这个
- server TGraph和Neo4j的server，把Kernel API封装成网络服务
- statistics 统计各数据集的特点代码