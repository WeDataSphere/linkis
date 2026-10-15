#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# JMX 文件路径变量化转换脚本
#
# 将 JMX 文件中的 Windows 本地绝对路径替换为 ${file_path} 变量引用，
# 使 JMX 测试计划可以在项目内通过 jmx-env-config.properties 配置路径。
#
# 转换规则（按顺序匹配，先长后短避免冲突）:
#   1. WEMETER2024/测试报告/  ->  ${summary_report_path}/
#   2. WEMETER2024/            ->  ${file_path}/validator/
#   3. uat-v1/测试报告/         ->  ${summary_report_path}/
#   4. uat-v1/                 ->  ${file_path}/udf/
#   5. sit测试/                ->  ${file_path}/scriptis/
#   6. 自动化测例/              ->  ${file_path}/production/
#   7. 造数工具/                ->  ${file_path}/scriptis/
#
# 用法:
#   python convert_paths.py           # 转换所有 JMX 文件
#   python convert_paths.py --check   # 仅检查不修改

import os
import sys
import glob
import re

# JMX 文件目录
JMX_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "回归")

# 路径替换规则（顺序敏感：先匹配长路径，后匹配短路径）
# 使用正则匹配，处理路径分隔符 \ 或 /
REPLACEMENT_RULES = [
    # 规则1: WEMETER2024\测试报告\  →  summary_report_path
    (
        r'D:\\大数据\\自动化\\功能测试\\WEMETER2024\\测试报告\\',
        r'${summary_report_path}/'
    ),
    # 规则2: WEMETER2024\  →  file_path/validator/
    (
        r'D:\\大数据\\自动化\\功能测试\\WEMETER2024\\',
        r'${file_path}/validator/'
    ),
    # 规则3: uat-v1\测试报告\  →  summary_report_path/
    (
        r'D:\\大数据\\自动化\\功能测试\\uat-v1\\测试报告\\',
        r'${summary_report_path}/'
    ),
    # 规则4: uat-v1\  →  file_path/udf/
    (
        r'D:\\大数据\\自动化\\功能测试\\uat-v1\\',
        r'${file_path}/udf/'
    ),
    # 规则5: sit测试\  →  file_path/scriptis/
    (
        r'D:\\大数据\\自动化\\功能测试\\sit测试\\',
        r'${file_path}/scriptis/'
    ),
    # 规则6: 自动化测例\  →  file_path/production/
    (
        r'D:\\大数据\\自动化测例\\',
        r'${file_path}/production/'
    ),
    # 规则7: 造数工具\  →  file_path/scriptis/
    (
        r'D:\\大数据\\自动化\\造数工具\\',
        r'${file_path}/scriptis/'
    ),
]


def convert_file(filepath, check_only=False):
    """转换单个 JMX 文件中的路径"""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content
    replacements = []

    for pattern, replacement in REPLACEMENT_RULES:
        matches = re.findall(pattern, content)
        if matches:
            count = len(matches)
            content = re.sub(pattern, replacement, content)
            replacements.append((pattern, replacement, count))

    if content != original_content:
        if not check_only:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
        return True, replacements
    return False, []


def main():
    check_only = '--check' in sys.argv

    if not os.path.isdir(JMX_DIR):
        print(f"ERROR: JMX directory not found: {JMX_DIR}")
        sys.exit(1)

    jmx_files = sorted(glob.glob(os.path.join(JMX_DIR, "*.jmx")))
    if not jmx_files:
        print(f"WARNING: No .jmx files found in {JMX_DIR}")
        sys.exit(0)

    print(f"{'[CHECK] ' if check_only else ''}JMX 路径变量化转换")
    print(f"目录: {JMX_DIR}")
    print(f"文件数: {len(jmx_files)}")
    print("=" * 60)

    total_replaced = 0
    total_files = 0

    for jmx_file in jmx_files:
        basename = os.path.basename(jmx_file)
        converted, replacements = convert_file(jmx_file, check_only)

        if converted:
            total_files += 1
            total_count = sum(r[2] for r in replacements)
            total_replaced += total_count
            status = "需转换" if check_only else "已转换"
            print(f"\n{status}: {basename} ({total_count} 处)")
            for pattern, replacement, count in replacements:
                # 简化显示
                short_pattern = pattern.replace('\\\\', '\\').replace('D:\\大数据\\自动化\\', '...')
                print(f"  {count:3d} x {short_pattern}  →  {replacement}")
        else:
            print(f"  无需转换: {basename}")

    print("\n" + "=" * 60)
    print(f"汇总: {total_files}/{len(jmx_files)} 个文件{'需要' if check_only else '已完成'}转换, 共 {total_replaced} 处替换")

    if check_only and total_files > 0:
        print("\n提示: 去掉 --check 参数执行实际转换")


if __name__ == '__main__':
    main()
