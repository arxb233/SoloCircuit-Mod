# SoloCircuit-Mod  
**MC单人生电辅助模组**  

## 简介  
- 单人玩生电太肝了，平时要上班，没有这么多时间去肝。

- 我决定编写这个模组，加入一些功能来优化生电的肝度。  

- 为了有效降低肝度，模组大部分功能其实破坏了生电的玩法，请酌情使用。

- 模组为服务端模组，因此破坏生电玩法功能使用前，需要op先设置指令 /sc cheat true，以表示已同意作弊 

---

## 功能列表  

- 背包清理 ：方便生电背包的清理和整理

- 潜影盒拆装 ：将背包中所有的物品打包成一个潜影盒或者反向拆开
 
- 便捷指令：自定义封装重定义指令
  
- 地狱门计算器：计算当前坐标的地狱对应坐标

> **以下功能破坏生电玩法，请酌情使用**
> - 以下功能使用前需要先设置/sc cheat true，以表示你已同意作弊 
> - **连锁砍树**：方便生存前期的资源积累。
> - **生存飞行**：方便建造机器和维修机器。
> - **快速挖掘**：修改挖掘速度
> - **伤害吸收**：调整玩家受伤扣血比例，生电玩家太脆了
> - **快速平地**：清空附近半径3个区块的高度比当前坐标高的方块
> -  **虚拟末影箱**：输入指令即可打开末影箱
> - **虚拟全物品**: 将物品进行上传，上传后进行计数扣除背包物品，自动备货的时候给予指定量的物品，对上传物品进行扣数
> - **自动备货**: 从虚拟全物品中根据投影清单自动装箱备货
> - **Fill指令上限调整**：调整Fill指令的上限，方便Fill的使用。
> - **清空控制域**：以当前位置区块为中心点，清空四个方向的指定半径的区块。
---

## 使用方法  

### 背包清理  
-  **命令**：`/trash add` 添加垃圾黑名单
-  **命令**：`/trash remove` 删除垃圾黑名单
-  **命令**：`/trash list` 查看垃圾黑名单
-  **命令**：`/trash clear` 清除背包黑名单中的物品
-  **命令**：`/trash clear all` 清除背包中所有的物品

### 潜影盒拆装
-  **命令**：`/box` 将背包中所有的物品打包成一个潜影盒
-  **命令**：`/box split` 将背包中潜影箱的物品拆包到背包，放不下的部分保留

### 便捷指令
-  **命令**：`/easy add <原版指令><自定义指令>` 添加自定义的指令
-  **命令**：`/easy list ` 查看已定义的指令
-  **命令**：`/easy remove <自定义指令>` 删除定义的指令
-  **命令**：`/easy <自定义指令> ` 使用自定义指令
  
### 地狱门计算器
-  **命令**：`/netherhere` 计算当前位置的地狱对应坐标
  
### 虚拟末影箱
-  **命令**：`/enderbox` 输入指令即可打开无实物末影箱

### 伤害吸收：
- **命令**：`/hurt set` 调整玩家受伤扣血比例

### 快速挖掘
-  **命令**：`/dig seed <速度值>` 设置挖掘速度
  
### 快速平地
-  **命令**：`/flat` 清空附近半径3个区块的高度比当前坐标高的方块
-  **命令**：`/flat <方块名称>` 并且替换坐标层的方块

### 虚拟全物品，自动备货
-  **命令**：`/material add` 添加虚拟物品并删除背包中或背包中潜影盒的物品
-  **命令**：`/material list ` 查看已存在的虚拟物品
-  **命令**：`/material remove <物品名称>` 将指定的物品从虚拟里删除，并将物品打一个潜影盒给用户
-  **命令**：`/material user` 获取投影文件的备货清单，将清单从虚拟物品中扣除，并将物品打足够的潜影盒给用户，不够直接返回缺那些

### 连锁砍树
- **命令**：`/tree` 使用钻石斧

### 生存飞行  
- 输入 `/fly` 开启或关闭飞行模式。  
- 使用 `/fly seed <速度值>` 设置飞行速度。  

### Fill指令上限调整 
- 使用命令 `/sc upadte fill <方块上限>`。  

### 清空控制域  
- 使用命令 `/skychunk <半径>` 清空以当前坐标所在区块的四个方向半径的所有区块
- 使用 `/skychunk <半径> air` 基岩层的基岩不保留。 

---

## 注意事项  
- 本模组旨在降低单人游戏的肝度，适合时间有限的玩家。  
- 请勿将本模组用于多人服务器，以免影响其他玩家的游戏体验。  
- 本项目仅供个人学习使用，请勿用于商业用途。  

---

## 结语  
希望这个模组能帮助你在有限的时间内更好地享受Minecraft的生电玩法。如果你有任何建议或反馈，欢迎提出！  
