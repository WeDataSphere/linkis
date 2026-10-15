#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# JMX 属性引用修复脚本
#
# 问题: ${file_path} 读取的是 JMeter 变量(variable), 但 -Jfile_path=... 设置的是属性(property)
#       JMeter 变量和属性是两个独立命名空间, ${file_path} 无法读取 -J 传入的属性
#
# 修复: 将 ${file_path} 改为 ${__P(file_path,)} 以读取 JMeter 属性
#       同理修复 ${summary_report_path}, ${global_summary_report_path}, ${filepath}
#
# 特殊处理: User Defined Variables 中的自引用问题
#   <elementProp name="file_path" ...>
#     <stringProp name="Argument.value">${file_path}/udf/</stringProp>
#   修复为:
#     <stringProp name="Argument.value">${__P(file_path,)}/udf/</stringProp>
#
# 第二类修复: User Defined Variables 定义中的硬编码环境值
#   <elementProp name="testenv" elementType="Argument">
#     <stringProp name="Argument.name">testenv</stringProp>
#     <stringProp name="Argument.value">sit.dss.bdap.weoa.com</stringProp>
#   修复为:
#     <stringProp name="Argument.value">${__P(testenv,)}</stringProp>
#   使其能从 -Jtestenv=... 属性读取, 无默认值, 统一从配置文件获取
#
# 第三类修复: HeaderManager 中 Token-Code / Token-User 的硬编码值
#   <elementProp name="" elementType="Header">
#     <stringProp name="Header.name">Token-Code</stringProp>
#     <stringProp name="Header.value">WDSL-LINKIS-AUTH-xxx</stringProp>
#   修复为:
#     <stringProp name="Header.value">${__P(token_code,)}</stringProp>
#   特殊 token (admin-kmsnd, LINKISCLI-AUTH 等) 使用各自独立的参数名
#   Token-User 同理, hadoop -> ${__P(token_user,)}, janicegong -> ${__P(admin_user,)} 等
#   无默认值, 统一从配置文件获取
#
# 第四类修复: 清理已有 JMX 中的 ${__P(var,默认值)} 改为 ${__P(var,)}
#   即前两类修复早期版本生成了带默认值的形式, 现统一改为无默认值
#
# 用法:
#   python fix_prop_refs.py           # 修复所有 JMX 文件
#   python fix_prop_refs.py --check   # 仅检查不修改

import os
import sys
import glob
import re

JMX_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "回归")

# 变量名 → __P() 函数调用
VAR_TO_PROP = {
    "file_path": "${__P(file_path,)}",
    "filepath": "${__P(filepath,)}",
    "summary_report_path": "${__P(summary_report_path,)}",
    "global_summary_report_path": "${__P(global_summary_report_path,)}",
}

# ============================================================
# 环境相关变量白名单: UDV 中定义的硬编码值需要改为 ${__P(varname,默认值)}
# 这些变量在 jmx-env-config.properties 中有对应配置, 通过 -J 参数传入
# 默认值采用各文件原有的硬编码值, 作为 fallback (属性未设置时使用)
# ============================================================
ENV_VAR_WHITELIST = {
    # 核心环境配置
    "testenv": None,           # Linkis 网关地址
    "usr": None,               # 主测试账号
    "pwd": None,               # 主测试账号密码
    "usr_admin": None,         # 管理员账号
    "pwd_admin": None,         # 管理员密码
    "usr_other": None,         # 其他账号
    "pwd_other": None,
    "usr_ordinary": None,      # 普通用户
    "pwd_ordinary": None,
    "usr_qj": None,
    "usr_depadmin": None,
    "usr_historyadmin": None,
    # Token 与认证
    "pubtokenName": None,
    "admin_token": None,
    "noauth_token": None,
    "personal_token": None,
    "other_token": None,
    "public_token": None,
    "public_token_long": None,
    "expire_token": None,
    "tokenName": None,
    "tokenName_new": None,
    # ECM 实例
    "ecm_instance": None,
    "ecminstance_bdp": None,
    "ecminstance_bdap": None,
    "ecminstance_bdap_ecm": None,
    # 租户与队列
    "tenant_default": None,
    "tenantlabel_default": None,
    "tenantlabel": None,
    "queue_master": None,
    "queue_slave": None,
    "queueName_other": None,
    # YARN 账号
    "yarn_usr": None,
    "yarn_pwd": None,
    # 工作空间
    "workspaceid": None,
    "workspaceid_noaisql": None,
    "workspaceName": None,
    "projectName": None,
    # 合法主机/IP 列表
    "legalHosts": None,
    "legalUsers": None,
    # Trino
    "trino_url": None,
    # 测试响应超时
    "LT": None,
}

# ============================================================
# HeaderManager 中 Token-Code 硬编码值 → __P() 参数名映射
# 出现频率高的用独立参数名, 特殊 token 各自一个参数
# 默认值采用原硬编码值, 作为 fallback
# ============================================================
TOKEN_CODE_MAP = {
    # 默认测试 token (出现 230 次, 覆盖全部 13 个文件)
    "WDSL-LINKIS-AUTH-r5YiFm4OtxhGf0jnkmB-TESTV1": "token_code",
    # 管理员 token (出现 19 次, 含尾部空格变体)
    "admin-kmsnd": "admin_token_code",
    "admin-kmsnd ": "admin_token_code",
    # CLI 认证 token (出现 11 次, 含尾部空格变体)
    "LINKISCLI-AUTH": "cli_auth_token",
    "LINKISCLI-AUTH ": "cli_auth_token",
    # 特殊测试 token (各 1-3 次, 用于异常/权限场景)
    "noexists": "noexists_token",
    "LINKIS_CLI_TEST": "linkis_cli_test_token",
    "test-api": "test_api_token",
    "STREAM-LOG ": "stream_log_token",
    "STREAM-LOG": "stream_log_token",
    "PACE-WIKI": "pace_wiki_token",
    "MASK-AUTH123": "mask_auth_token",
}

# ============================================================
# HeaderManager 中 Token-User 硬编码值 → __P() 参数名映射
# hadoop 是默认用户(284次), janicegong 是管理员用户(27次),
# 其他特殊用户各自独立参数
# ============================================================
TOKEN_USER_MAP = {
    # 默认测试用户 (出现 284 次, 覆盖全部 13 个文件)
    "hadoop": "token_user",
    # 管理员用户 (出现 27 次, 主要在参数模板/管理台测试)
    "janicegong": "admin_user",
    # 特殊测试用户 (各 1-5 次, 用于权限/多用户场景)
    "neiljianliu": "neiljianliu_user",
    "v_kkhuang": "v_kkhuang_user",
    "allenlliu": "allenlliu_user",
    "hduser05": "hduser05_user",
    "hduser0309": "hduser0309_user",
    "johnnwang": "johnnwang_user",
    "enjoyyin": "enjoyyin_user",
}


def fix_file(filepath, check_only=False):
    """修复单个 JMX 文件中的属性引用"""
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    fixes = []

    for var_name, prop_ref in VAR_TO_PROP.items():
        # 匹配 ${var_name} 但不匹配 ${__P(var_name,...)} (避免重复修复)
        # 也不匹配 ${var_name} 前面已经 __P 的情况
        pattern = r"\$\{" + re.escape(var_name) + r"\}"
        # 排除已经在 __P() 内部的匹配 (如 ${__P(file_path,)} 中的 file_path)
        # 通过检查前面是否有 __P( 来排除
        matches = []
        for m in re.finditer(pattern, content):
            # 检查前面 20 个字符是否有 __P(
            start = max(0, m.start() - 20)
            prefix = content[start:m.start()]
            if "__P(" not in prefix:
                matches.append(m)

        if matches:
            count = len(matches)
            # 逐个替换 (避免替换 __P() 内部的)
            for m in reversed(matches):
                content = content[:m.start()] + prop_ref + content[m.end():]
            fixes.append((var_name, prop_ref, count))

    if content != original_content:
        if not check_only:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(content)
        return True, fixes
    return False, []


def fix_udv_definitions(filepath, check_only=False):
    """修复 User Defined Variables 定义中的硬编码环境值

    将 UDV 中白名单变量的硬编码值替换为 ${__P(varname,原值)} 形式,
    使其能从 -Jvarname=... 属性读取, 原值保留为默认值 fallback.

    匹配模式:
      <elementProp name="testenv" elementType="Argument">
        <stringProp name="Argument.name">testenv</stringProp>
        <stringProp name="Argument.value">sit.dss.bdap.weoa.com</stringProp>
      处理后:
        <stringProp name="Argument.value">${__P(testenv,sit.dss.bdap.weoa.com)}</stringProp>

    跳过情况:
      1. 值已经是 ${__P(...)} 形式 (避免重复修复)
      2. 值包含 JMeter 函数如 ${__time}, ${__UUID}, ${__Random} (动态生成值)
      3. 值包含 ${其他变量} 引用 (变量组合, 不是纯硬编码)
      4. 值为空 (空字符串)
      5. 变量名不在白名单中
    """
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    fixes = []

    # 匹配 UDV 定义块:
    #   <elementProp name="VARNAME" elementType="Argument">
    #     <stringProp name="Argument.name">VARNAME</stringProp>
    #     <stringProp name="Argument.value">VALUE</stringProp>
    # 用正则捕获 VARNAME 和 VALUE
    # 注意: VALUE 可能包含特殊字符如 @, :, /, . 等, 但不应包含 </stringProp>
    pattern = re.compile(
        r'<elementProp\s+name="([^"]+)"\s+elementType="Argument">\s*'
        r'<stringProp\s+name="Argument\.name">\1</stringProp>\s*'
        r'<stringProp\s+name="Argument\.value">([^<]*)</stringProp>'
    )

    def replace_udv_value(match):
        var_name = match.group(1)
        original_value = match.group(2)

        # 只处理白名单中的变量
        if var_name not in ENV_VAR_WHITELIST:
            return match.group(0)

        # 跳过空值
        if not original_value.strip():
            return match.group(0)

        # 跳过已经是 __P() 形式的
        if "${__P(" in original_value:
            return match.group(0)

        # 跳过包含其他 JMeter 函数的值 (${__time}, ${__UUID}, ${__Random} 等)
        if re.search(r"\$\{__\w+\(", original_value):
            return match.group(0)

        # 跳过包含 ${其他变量} 引用的 (变量组合, 不是纯硬编码)
        # 但允许 ${__P(...)} 已经在上面跳过了
        if re.search(r"\$\{[^_]", original_value):
            return match.group(0)

        # 构造新值: ${__P(varname,)} 无默认值, 统一从配置文件获取
        new_value = "${__P(" + var_name + ",)}"

        # 记录修复
        fixes.append((var_name, original_value, new_value))

        # 用 group(2) 的 span 精准替换 value 内容 (保留所有其他部分)
        # match.start(2) 是 VALUE 内容开始位置, match.end(2) 是结束位置
        return match.group(0)[: match.start(2) - match.start(0)] + new_value + match.group(0)[match.end(2) - match.start(0) :]

    content = pattern.sub(replace_udv_value, content)

    if content != original_content:
        if not check_only:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(content)
        return True, fixes
    return False, []


def fix_header_manager_tokens(filepath, check_only=False):
    """修复 HeaderManager 中 Token-Code / Token-User 的硬编码值

    将 HeaderManager 中的硬编码 token 和 user 值替换为 ${__P(参数名,原值)} 形式,
    使其能从 -J参数名=... 属性读取, 原值保留为默认值 fallback.

    匹配模式 (HeaderManager 中的 header 条目):
      <elementProp name="" elementType="Header">
        <stringProp name="Header.name">Token-Code</stringProp>
        <stringProp name="Header.value">WDSL-LINKIS-AUTH-xxx</stringProp>
      处理后:
        <stringProp name="Header.value">${__P(token_code,WDSL-LINKIS-AUTH-xxx)}</stringProp>

    跳过情况:
      1. 值已经是 ${__P(...)} 形式 (避免重复修复)
      2. 值已经是 ${变量名} 引用 (如 ${pubtokenName}, ${tokenName} 等)
      3. 值包含 JMeter 函数如 ${__time}, ${__UUID} 等
      4. 值为空
      5. 值不在 TOKEN_CODE_MAP / TOKEN_USER_MAP 中
    """
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    fixes = []

    # 合并 Token-Code 和 Token-User 的映射, 用 header_name 区分
    # 格式: { "Token-Code": TOKEN_CODE_MAP, "Token-User": TOKEN_USER_MAP }
    header_maps = {
        "Token-Code": TOKEN_CODE_MAP,
        "Token-User": TOKEN_USER_MAP,
    }

    for header_name, value_map in header_maps.items():
        # 匹配 HeaderManager 中的 header 条目:
        #   <elementProp name="..." elementType="Header">
        #     <stringProp name="Header.name">HEADER_NAME</stringProp>
        #     <stringProp name="Header.value">VALUE</stringProp>
        # 用正则捕获 VALUE, 注意 VALUE 不应包含 </stringProp>
        pattern = re.compile(
            r'(<elementProp\s+name="[^"]*"\s+elementType="Header">\s*'
            r'<stringProp\s+name="Header\.name">'
            + re.escape(header_name)
            + r'</stringProp>\s*'
            r'<stringProp\s+name="Header\.value">)([^<]*)(</stringProp>)',
            re.DOTALL,
        )

        def make_replacer(h_name, v_map):
            def replace_header_value(match):
                prefix = match.group(1)  # <elementProp...><stringProp...>Token-Code</stringProp><stringProp...>
                original_value = match.group(2)  # VALUE 内容
                suffix = match.group(3)  # </stringProp>

                # 跳过空值
                if not original_value.strip():
                    return match.group(0)

                # 跳过已经是 __P() 形式的
                if "${__P(" in original_value:
                    return match.group(0)

                # 跳过包含其他 JMeter 函数的值
                if re.search(r"\$\{__\w+\(", original_value):
                    return match.group(0)

                # 跳过已经是 ${变量名} 引用的 (如 ${pubtokenName}, ${usr} 等)
                if re.search(r"\$\{[^_]", original_value):
                    return match.group(0)

                # 查找映射, 注意尾部空格的变体已在 map 中统一处理
                # 先精确匹配, 再 strip 后匹配
                param_name = v_map.get(original_value)
                if param_name is None:
                    # 尝试 strip 后匹配 (处理尾部空格)
                    param_name = v_map.get(original_value.strip())
                    if param_name is None:
                        return match.group(0)

                # 构造新值: ${__P(参数名,)} 无默认值, 统一从配置文件获取
                new_value = "${__P(" + param_name + ",)}"

                # 记录修复
                fixes.append((h_name, original_value, param_name, new_value))

                return prefix + new_value + suffix

            return replace_header_value

        content = pattern.sub(make_replacer(header_name, value_map), content)

    if content != original_content:
        if not check_only:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(content)
        return True, fixes
    return False, []


def strip_prop_defaults(filepath, check_only=False):
    """清理已有 JMX 中的 ${__P(var,默认值)} 改为 ${__P(var,)}

    前两类修复早期版本生成了带默认值的形式 ${__P(varname,默认值)},
    现统一改为无默认值 ${__P(varname,)}, 统一从配置文件获取.

    匹配并替换:
      ${__P(testenv,sit.dss.bdap.weoa.com)}  ->  ${__P(testenv,)}
      ${__P(token_code,WDSL-LINKIS-AUTH-xxx)}  ->  ${__P(token_code,)}
      ${__P(file_path,/some/path)}  ->  ${__P(file_path,)}

    跳过情况:
      1. 已经是 ${__P(var,)} 形式 (无默认值, 不处理)
      2. ${__P(var)} 形式 (无逗号, 不处理 - 虽然不标准但 JMeter 支持)
    """
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    original_content = content
    fixes = []

    # 匹配 ${__P(varname,默认值)} 形式, 替换为 ${__P(varname,)}
    # 默认值部分不能包含 } 字符 (否则会匹配到错误的边界)
    # 也不能包含 ${ (避免匹配嵌套变量引用)
    pattern = re.compile(r"\$\{__P\(([^,${}]+),[^}]*\)\}")

    def replace_default(match):
        var_name = match.group(1)
        old_form = match.group(0)
        new_form = "${__P(" + var_name + ",)}"
        fixes.append((var_name, old_form, new_form))
        return new_form

    content = pattern.sub(replace_default, content)

    if content != original_content:
        if not check_only:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(content)
        return True, fixes
    return False, []


def main():
    check_only = "--check" in sys.argv

    if not os.path.isdir(JMX_DIR):
        print(f"ERROR: JMX directory not found: {JMX_DIR}")
        sys.exit(1)

    jmx_files = sorted(glob.glob(os.path.join(JMX_DIR, "*.jmx")))
    if not jmx_files:
        print(f"WARNING: No .jmx files found in {JMX_DIR}")
        sys.exit(0)

    print(f"{'[CHECK] ' if check_only else ''}JMX 属性引用修复")
    print(f"目录: {JMX_DIR}")
    print(f"文件数: {len(jmx_files)}")
    print("=" * 60)

    # ---- 第一类: 引用修复 ${var} -> ${__P(var,)} ----
    print("\n[1/4] 属性引用修复: ${var} -> ${__P(var,)}")
    print("-" * 60)

    total_fixed_ref = 0
    total_files_ref = 0

    for jmx_file in jmx_files:
        basename = os.path.basename(jmx_file)
        fixed, fixes = fix_file(jmx_file, check_only)

        if fixed:
            total_files_ref += 1
            total_count = sum(f[2] for f in fixes)
            total_fixed_ref += total_count
            status = "需修复" if check_only else "已修复"
            print(f"\n{status}: {basename} ({total_count} 处)")
            for var_name, prop_ref, count in fixes:
                print(f"  {count:3d} x ${{{var_name}}}  ->  {prop_ref}")
        else:
            print(f"  无需修复: {basename}")

    # ---- 第二类: UDV 定义修复 (硬编码值 -> ${__P(varname,)}) ----
    print("\n[2/4] UDV 定义修复: 硬编码值 -> ${__P(varname,)}")
    print("-" * 60)

    total_fixed_udv = 0
    total_files_udv = 0

    for jmx_file in jmx_files:
        basename = os.path.basename(jmx_file)
        fixed, fixes = fix_udv_definitions(jmx_file, check_only)

        if fixed:
            total_files_udv += 1
            total_fixed_udv += len(fixes)
            status = "需修复" if check_only else "已修复"
            print(f"\n{status}: {basename} ({len(fixes)} 处)")
            for var_name, old_val, new_val in fixes:
                # 截断显示, 避免过长
                old_display = old_val if len(old_val) <= 40 else old_val[:37] + "..."
                print(f"  {var_name}: {old_display}")
        else:
            print(f"  无需修复: {basename}")

    # ---- 第三类: HeaderManager token/user 修复 ----
    print("\n[3/4] HeaderManager token/user 修复: 硬编码值 -> ${__P(参数名,)}")
    print("-" * 60)

    total_fixed_hdr = 0
    total_files_hdr = 0

    for jmx_file in jmx_files:
        basename = os.path.basename(jmx_file)
        fixed, fixes = fix_header_manager_tokens(jmx_file, check_only)

        if fixed:
            total_files_hdr += 1
            total_fixed_hdr += len(fixes)
            status = "需修复" if check_only else "已修复"
            print(f"\n{status}: {basename} ({len(fixes)} 处)")
            # 按参数名分组统计
            param_counts = {}
            for h_name, old_val, param_name, new_val in fixes:
                key = f"{h_name} -> {param_name}"
                if key not in param_counts:
                    param_counts[key] = {"count": 0, "sample": old_val}
                param_counts[key]["count"] += 1
            for key, info in sorted(param_counts.items()):
                sample_display = info["sample"] if len(info["sample"]) <= 40 else info["sample"][:37] + "..."
                print(f"  {info['count']:3d} x {key}  (如: {sample_display})")
        else:
            print(f"  无需修复: {basename}")

    # ---- 第四类: 清理已有 ${__P(var,默认值)} -> ${__P(var,)} ----
    print("\n[4/4] 清理默认值: ${__P(var,默认值)} -> ${__P(var,)}")
    print("-" * 60)

    total_fixed_strip = 0
    total_files_strip = 0

    for jmx_file in jmx_files:
        basename = os.path.basename(jmx_file)
        fixed, fixes = strip_prop_defaults(jmx_file, check_only)

        if fixed:
            total_files_strip += 1
            total_fixed_strip += len(fixes)
            status = "需修复" if check_only else "已清理"
            print(f"\n{status}: {basename} ({len(fixes)} 处)")
            # 按变量名分组统计
            var_counts = {}
            for var_name, old_form, new_form in fixes:
                if var_name not in var_counts:
                    var_counts[var_name] = 0
                var_counts[var_name] += 1
            for var_name, count in sorted(var_counts.items()):
                print(f"  {count:3d} x {var_name}")
        else:
            print(f"  无需修复: {basename}")

    # ---- 汇总 ----
    print("\n" + "=" * 60)
    print(
        f"汇总 [引用修复]: {total_files_ref}/{len(jmx_files)} 个文件"
        f"{'需要' if check_only else '已完成'}修复, 共 {total_fixed_ref} 处"
    )
    print(
        f"汇总 [UDV修复]: {total_files_udv}/{len(jmx_files)} 个文件"
        f"{'需要' if check_only else '已完成'}修复, 共 {total_fixed_udv} 处"
    )
    print(
        f"汇总 [Header修复]: {total_files_hdr}/{len(jmx_files)} 个文件"
        f"{'需要' if check_only else '已完成'}修复, 共 {total_fixed_hdr} 处"
    )
    print(
        f"汇总 [清理默认值]: {total_files_strip}/{len(jmx_files)} 个文件"
        f"{'需要' if check_only else '已完成'}清理, 共 {total_fixed_strip} 处"
    )

    if check_only and (total_files_ref > 0 or total_files_udv > 0 or total_files_hdr > 0 or total_files_strip > 0):
        print("\n提示: 去掉 --check 参数执行实际修复")


if __name__ == "__main__":
    main()
