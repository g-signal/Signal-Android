# Signal-Android settings.gradle.kts 完整解决方案

## 📦 已创建的文件

### 配置文件

| 文件 | 说明 | 用途 |
|------|------|------|
| `settings.gradle.kts` | 原始官方版本 | 当前使用，生产环境 |
| `settings.gradle.kts.reference` | 详细注释版 | 学习参考，理解原理 |
| `settings.gradle.kts.enhanced` | 增强调试版 | 开发调试，自动诊断 |

### 工具脚本

| 文件 | 功能 | 使用场景 |
|------|------|---------|
| `setup_local_libsignal.bat` | 启用本地 libsignal | 首次配置 |
| `restore_official_libsignal.bat` | 恢复官方 libsignal | 切换回官方版本 |
| `switch_settings_version.bat` | 切换 settings 版本 | 在不同版本间切换 |
| `check_libsignal_status.bat` | 检查配置状态 | 诊断问题 |

### 文档

| 文件 | 内容 |
|------|------|
| `SETTINGS_GRADLE_GUIDE.md` | 详细使用指南 |
| `README_SETTINGS.md` | 本文档 |

---

## 🚀 快速开始

### 场景 1: 第一次配置使用本地 libsignal

```cmd
# 步骤 1: 启用本地 libsignal
setup_local_libsignal.bat

# 步骤 2: 检查状态（可选）
check_libsignal_status.bat

# 步骤 3: 构建项目
gradlew assemblePlayProdDebug
```

### 场景 2: 切换到增强版本（用于调试）

```cmd
# 切换版本
switch_settings_version.bat
# 选择 2) Enhanced

# 同步项目
gradlew --refresh-dependencies
```

### 场景 3: 检查当前配置

```cmd
check_libsignal_status.bat
```

### 场景 4: 恢复官方 libsignal

```cmd
restore_official_libsignal.bat
```

---

## 📊 版本对比

### 原始版本 (settings.gradle.kts)

**优点**:
- ✅ Signal 官方维护
- ✅ 简洁稳定
- ✅ 适合生产环境

**缺点**:
- ❌ 无错误诊断
- ❌ 配置问题不明显
- ❌ 无中文注释

**适用场景**: 生产构建、发布版本

---

### 参考版本 (settings.gradle.kts.reference)

**优点**:
- ✅ 详细中文注释
- ✅ 解释每个配置的作用
- ✅ 包含使用示例
- ✅ 故障排查指南

**缺点**:
- ⚠️ 仅供参考，不用于实际构建

**适用场景**: 学习理解、技术文档

**查看方式**:
```cmd
notepad settings.gradle.kts.reference
```

---

### 增强版本 (settings.gradle.kts.enhanced)

**优点**:
- ✅ 自动验证配置
- ✅ 检查 native 库状态
- ✅ 显示架构信息
- ✅ 详细错误提示
- ✅ 实时诊断

**示例输出**:
```
======================================================================
📦 Using LOCAL libsignal
======================================================================
Path: E:\code\libsignal\java
Native libs (.so): ✅ Found
Architectures: arm64-v8a, armeabi-v7a, x86, x86_64
======================================================================
```

**适用场景**: 开发调试、问题诊断

**启用方式**:
```cmd
switch_settings_version.bat
# 选择 2) Enhanced
```

---

## 🔧 工具脚本详解

### setup_local_libsignal.bat

**功能**:
1. 检查是否已配置
2. 修改 gradle.properties
3. 添加必要的配置项
4. 清理构建缓存

**执行效果**:
```properties
# 在 gradle.properties 中添加：
libsignalClientPath=../libsignal
org.gradle.dependency.verification=lenient
```

---

### restore_official_libsignal.bat

**功能**:
1. 注释掉本地配置
2. 清理构建缓存
3. 恢复使用 Maven 仓库

**执行效果**:
```properties
# 在 gradle.properties 中注释：
# libsignalClientPath=../libsignal
# org.gradle.dependency.verification=lenient
```

---

### switch_settings_version.bat

**功能**:
- 在不同 settings.gradle.kts 版本间切换
- 自动创建备份
- 可恢复到原始版本

**交互菜单**:
```
1) Original  - 官方版本
2) Enhanced  - 增强版本
3) Reference - 查看参考版本
4) Restore from backup - 从备份恢复
```

---

### check_libsignal_status.bat

**功能**:
- 检查 gradle.properties 配置
- 验证 libsignal 路径
- 检查子项目完整性
- 扫描 native 库
- 显示架构和文件大小

**示例输出**:
```
[1/4] Checking gradle.properties...
  Status: ✅ Local libsignal ENABLED
  Path: ../libsignal

[2/4] Checking libsignal project...
  Path exists: ✅ E:\code\Signal-Android\..\libsignal\java

[3/4] Checking subprojects...
  client project: ✅ Found
  android project: ✅ Found

[4/4] Checking native libraries (.so files)...
  jniLibs directory: ✅ Found

  Architectures:
    - arm64-v8a: ✅ libsignal_jni.so (63 MB)
    - armeabi-v7a: ✅ libsignal_jni.so (58 MB)
    - x86: ✅ libsignal_jni.so (59 MB)
    - x86_64: ✅ libsignal_jni.so (66 MB)
```

---

## 🎯 常见使用流程

### 开发流程

```
┌─────────────────────────┐
│ setup_local_libsignal   │  ← 首次配置
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ check_libsignal_status  │  ← 验证配置
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ switch_settings_version │  ← 切换到增强版（可选）
│      (选择 Enhanced)     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ 修改 libsignal 代码     │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ 重新构建 .so 文件       │
│ cd libsignal/java       │
│ bash build_jni.sh       │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ 构建 Signal-Android     │
│ gradlew assembleDe...   │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ 测试 APK                │
└─────────────────────────┘
```

### 发布前流程

```
┌─────────────────────────┐
│ switch_settings_version │  ← 切换回原始版本
│      (选择 Original)    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│restore_official_libsignal│ ← 恢复官方 libsignal
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ gradlew clean           │  ← 清理构建
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│ 构建发布版本            │
└─────────────────────────┘
```

---

## 📖 文档索引

### 快速入门
- **本文档**: `README_SETTINGS.md` - 总体概览
- **快速参考**: `E:\code\QUICK_REFERENCE.md` - 整个项目的快速参考

### 详细指南
- **settings.gradle.kts 指南**: `SETTINGS_GRADLE_GUIDE.md` - 配置详解
- **集成指南**: `E:\code\libsignal\INTEGRATION_GUIDE_CN.md` - libsignal 集成

### 配置参考
- **带注释版本**: `settings.gradle.kts.reference` - 学习和理解

---

## 🔍 故障排查

### 问题：无法找到 libsignal

**检查步骤**:
```cmd
check_libsignal_status.bat
```

查看输出，找到具体问题。

---

### 问题：native 库缺失

**解决方法**:
```bash
cd E:\code\libsignal\java
bash build_jni.sh android
```

然后重新运行状态检查。

---

### 问题：不确定当前使用哪个版本

**查看当前配置**:
```cmd
check_libsignal_status.bat
```

**查看 settings 版本**:
```cmd
# 检查文件差异
fc settings.gradle.kts settings.gradle.kts.enhanced
```

---

## 💡 最佳实践

### 1. 开发时使用增强版本

```cmd
switch_settings_version.bat  # 选择 2
```

好处：
- 快速发现配置问题
- 实时查看 libsignal 状态
- 详细的错误提示

### 2. 定期运行状态检查

```cmd
check_libsignal_status.bat
```

在以下情况运行：
- 修改配置后
- 重新构建 libsignal 后
- 遇到构建问题时

### 3. 发布前恢复官方版本

```cmd
restore_official_libsignal.bat
switch_settings_version.bat  # 选择 1
```

确保：
- 使用官方稳定版本
- 配置简洁
- 避免调试输出

### 4. 备份重要配置

所有脚本都会自动创建备份：
- `settings.gradle.kts.backup`
- 可以随时恢复

---

## 📞 获取帮助

### 查看文档
```cmd
# 查看设置指南
type SETTINGS_GRADLE_GUIDE.md

# 查看集成指南
type E:\code\libsignal\INTEGRATION_GUIDE_CN.md

# 查看快速参考
type E:\code\QUICK_REFERENCE.md
```

### 使用工具诊断
```cmd
# 检查配置状态
check_libsignal_status.bat

# 查看 Gradle 依赖树
gradlew :app:dependencies
```

---

## ✅ 检查清单

使用本文档前，确认：

- [ ] libsignal 已构建（4个架构，8个 .so 文件）
- [ ] .so 文件在 `libsignal/java/android/src/main/jniLibs/`
- [ ] 了解三个 settings.gradle.kts 版本的区别
- [ ] 知道如何使用工具脚本
- [ ] 可以运行状态检查

一切就绪后，开始使用！

---

**最后更新**: 2025-10-14
**版本**: 1.0
**作者**: Claude Code Assistant
