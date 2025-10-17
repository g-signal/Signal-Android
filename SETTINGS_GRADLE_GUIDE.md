# settings.gradle.kts 配置指南

## 文件说明

我为您创建了三个版本的 `settings.gradle.kts` 文件：

### 1. `settings.gradle.kts` (原始版本)
- **位置**: `E:\code\Signal-Android\settings.gradle.kts`
- **状态**: 当前使用的版本
- **特点**: Signal 官方原版配置
- **建议**: 保持不变，除非需要自定义

### 2. `settings.gradle.kts.reference` (参考版本)
- **位置**: `E:\code\Signal-Android\settings.gradle.kts.reference`
- **特点**: 包含详细中文注释，解释每个配置的作用
- **用途**:
  - 学习和理解配置
  - 查看 libsignal 集成原理
  - 作为配置参考手册

### 3. `settings.gradle.kts.enhanced` (增强版本)
- **位置**: `E:\code\Signal-Android\settings.gradle.kts.enhanced`
- **特点**:
  - 自动验证 libsignal 路径
  - 检查 native 库是否已构建
  - 显示详细的配置状态
  - 更好的错误提示
- **用途**: 调试和开发

---

## 增强版本的优势

### 自动诊断功能

当您运行 Gradle 同步或构建时，增强版本会显示：

#### 使用官方 libsignal 时：
```
======================================================================
📦 Using OFFICIAL libsignal from Maven repositories
======================================================================
To use local libsignal, set in gradle.properties:
  libsignalClientPath=../libsignal
  org.gradle.dependency.verification=lenient
Or run: setup_local_libsignal.bat
======================================================================
```

#### 使用本地 libsignal 时（成功）：
```
======================================================================
📦 Using LOCAL libsignal
======================================================================
Path: E:\code\libsignal\java
Native libs (.so): ✅ Found
Architectures: arm64-v8a, armeabi-v7a, x86, x86_64
======================================================================
```

#### 使用本地 libsignal 时（缺少 native 库）：
```
======================================================================
📦 Using LOCAL libsignal
======================================================================
Path: E:\code\libsignal\java
Native libs (.so): ⚠️  Not found

⚠️  WARNING: Native libraries (.so files) not found!
Please run: cd E:\code\libsignal && ./build_jni.sh android
Or run: E:\code\libsignal\java\build_jni.sh android

======================================================================
```

---

## 如何使用增强版本

### 选项 1: 替换现有文件（推荐用于开发）

```bash
cd E:\code\Signal-Android

# 备份原始文件
copy settings.gradle.kts settings.gradle.kts.backup

# 使用增强版本
copy settings.gradle.kts.enhanced settings.gradle.kts
```

### 选项 2: 保持原版（推荐用于生产）

不做任何改动，当前的 `settings.gradle.kts` 已经足够使用。

---

## 配置 libsignal 路径

无论使用哪个版本，配置方法都相同：

### 方法 A: 使用自动化脚本（最简单）

```cmd
setup_local_libsignal.bat
```

### 方法 B: 手动编辑 gradle.properties

在 `E:\code\Signal-Android\gradle.properties` 中添加：

```properties
libsignalClientPath=../libsignal
org.gradle.dependency.verification=lenient
```

### 方法 C: 使用绝对路径

```properties
libsignalClientPath=E:/code/libsignal
org.gradle.dependency.verification=lenient
```

---

## 验证配置

### 1. 查看 Gradle 输出

运行任何 Gradle 命令，查看输出：

```bash
gradlew projects
```

应该看到类似输出：
```
📦 Using LOCAL libsignal
Path: E:\code\libsignal\java
Native libs (.so): ✅ Found
...
```

### 2. 检查依赖

```bash
gradlew :app:dependencies --configuration debugRuntimeClasspath | findstr libsignal
```

使用本地版本时，应该看到 `project :android` 而不是 `org.signal:libsignal-android:x.x.x`

---

## 故障排查

### 问题 1: 路径找不到

**症状**:
```
⚠️  WARNING: libsignal path does not exist: E:\code\libsignal\java
```

**解决**:
1. 检查 libsignal 项目是否存在
2. 确认路径使用正斜杠 `/` 而不是反斜杠 `\`
3. 尝试使用绝对路径

### 问题 2: Native 库缺失

**症状**:
```
Native libs (.so): ⚠️  Not found
```

**解决**:
```bash
cd E:\code\libsignal
cd java
bash build_jni.sh android
```

### 问题 3: 子项目不完整

**症状**:
```
⚠️  WARNING: libsignal projects incomplete
```

**解决**:
- 确认 libsignal 项目完整性
- 重新 clone libsignal 仓库
- 检查 `libsignal/java/client/build.gradle` 是否存在

---

## 对比表格

| 特性 | 原始版本 | 参考版本 | 增强版本 |
|------|---------|---------|---------|
| 功能完整性 | ✅ | ✅ | ✅ |
| 中文注释 | ❌ | ✅ | ⚠️ 部分 |
| 路径验证 | ❌ | ❌ | ✅ |
| native 库检查 | ❌ | ❌ | ✅ |
| 架构信息显示 | ❌ | ❌ | ✅ |
| 调试输出 | ❌ | ❌ | ✅ |
| 错误提示 | ⚠️ 基础 | ⚠️ 基础 | ✅ 详细 |
| 适用场景 | 生产环境 | 学习参考 | 开发调试 |

---

## 推荐用法

### 开发阶段
使用 **增强版本** (`settings.gradle.kts.enhanced`)
- 可以快速发现配置问题
- 实时查看 libsignal 状态
- 获得详细的错误提示

### 生产环境
使用 **原始版本** (`settings.gradle.kts`)
- 简洁，没有额外的输出
- Signal 官方维护
- 稳定可靠

### 学习研究
参考 **参考版本** (`settings.gradle.kts.reference`)
- 详细的中文注释
- 完整的配置说明
- 使用场景示例

---

## 快速命令参考

### 切换到增强版本
```cmd
cd E:\code\Signal-Android
copy settings.gradle.kts settings.gradle.kts.backup
copy settings.gradle.kts.enhanced settings.gradle.kts
```

### 恢复原始版本
```cmd
cd E:\code\Signal-Android
copy settings.gradle.kts.backup settings.gradle.kts
```

### 查看当前使用的 libsignal 源
```bash
gradlew projects | findstr libsignal
```

### 强制刷新依赖
```bash
gradlew --refresh-dependencies
```

---

## 注意事项

1. **备份重要**: 修改前务必备份原始文件
2. **路径格式**: Windows 上使用正斜杠 `/` 或双反斜杠 `\\`
3. **清理缓存**: 切换 libsignal 源后运行 `gradlew clean`
4. **版本兼容**: 确保 Signal-Android 和 libsignal 版本匹配

---

## 相关文档

- **集成指南**: `E:\code\libsignal\INTEGRATION_GUIDE_CN.md`
- **快速参考**: `E:\code\QUICK_REFERENCE.md`
- **自动化脚本**:
  - `E:\code\Signal-Android\setup_local_libsignal.bat`
  - `E:\code\Signal-Android\restore_official_libsignal.bat`
