#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# JMX 失败请求排查脚本
#
# 用途：从 XML 格式的 JTL 文件中提取失败请求的完整请求/响应数据，生成可读的报告
#       用于排查接口失败的具体原因（请求参数、响应体、断言失败信息）
#
# 用法：
#   python parse_jtl_failures.py <jtl_file>                 # 提取所有失败请求
#   python parse_jtl_failures.py <jtl_file> --all           # 提取所有请求（含成功）
#   python parse_jtl_failures.py <jtl_file> --label <关键词> # 按标签关键词过滤
#   python parse_jtl_failures.py <jtl_file> --output <file> # 输出到文件
#
# 输出内容（每个失败请求）：
#   - 标签 (label)
#   - 响应码 (responseCode)
#   - 响应消息 (responseMessage)
#   - 耗时 (elapsed)
#   - 请求方法 (method)
#   - 请求头 (requestHeaders) - 含 Token-Code, Token-User 等
#   - 请求体 (queryString) - POST body
#   - 响应头 (responseHeaders)
#   - 响应体 (responseData) - 服务端返回内容
#   - 断言结果 (assertionResult) - 断言失败信息

import re
import sys
import os
import argparse
from html import unescape


def parse_jtl(jtl_file):
    """解析 XML JTL 文件，提取所有样本"""
    with open(jtl_file, "r", encoding="utf-8", errors="replace") as f:
        content = f.read()

    # 用正则提取每个 httpSample / sample 块
    # 匹配 <httpSample ...>...</httpSample> 或 <httpSample .../> (自闭合)
    samples = []
    # 先匹配非自闭合的完整块
    for m in re.finditer(
        r"<(httpSample|sample)\s+([^>]+)>(.*?)</\1>", content, re.DOTALL
    ):
        tag = m.group(1)
        attrs_str = m.group(2)
        body = m.group(3)
        attrs = dict(re.findall(r'(\w+)="([^"]*)"', attrs_str))
        samples.append({"tag": tag, "attrs": attrs, "body": body})

    # 再匹配自闭合的 <httpSample ... />
    for m in re.finditer(r"<(httpSample|sample)\s+([^>]+)/>", content):
        tag = m.group(1)
        attrs_str = m.group(2)
        attrs = dict(re.findall(r'(\w+)="([^"]*)"', attrs_str))
        samples.append({"tag": tag, "attrs": attrs, "body": ""})

    return samples


def extract_body_field(body, field_name):
    """从 sample body 中提取子字段（如 responseHeader, requestHeader, responseData 等）"""
    # 匹配 <field_name class="...">内容</field_name> 或 <field_name>内容</field_name>
    pattern = r"<" + field_name + r'(?:\s+class="[^"]*")?>(.*?)</' + field_name + r">"
    m = re.search(pattern, body, re.DOTALL)
    if m:
        return unescape(m.group(1).strip())
    return ""


def extract_assertions(body):
    """提取断言结果"""
    assertions = []
    for m in re.finditer(
        r"<assertionResult>(.*?)</assertionResult>", body, re.DOTALL
    ):
        assertion_body = m.group(1)
        name = ""
        failure = "false"
        error = "false"
        failure_message = ""
        name_m = re.search(r"<name>(.*?)</name>", assertion_body, re.DOTALL)
        if name_m:
            name = unescape(name_m.group(1).strip())
        failure_m = re.search(r"<failure>(.*?)</failure>", assertion_body, re.DOTALL)
        if failure_m:
            failure = failure_m.group(1).strip()
        error_m = re.search(r"<error>(.*?)</error>", assertion_body, re.DOTALL)
        if error_m:
            error = error_m.group(1).strip()
        msg_m = re.search(
            r"<failureMessage>(.*?)</failureMessage>", assertion_body, re.DOTALL
        )
        if msg_m:
            failure_message = unescape(msg_m.group(1).strip())
        assertions.append(
            {
                "name": name,
                "failure": failure,
                "error": error,
                "failureMessage": failure_message,
            }
        )
    return assertions


def format_sample(sample, index):
    """格式化单个样本为可读文本"""
    attrs = sample["attrs"]
    body = sample["body"]

    label = attrs.get("lb", "")
    rc = attrs.get("rc", "")
    rm = attrs.get("rm", "")
    t = attrs.get("t", "")
    ts = attrs.get("ts", "")
    success = attrs.get("s", "")

    # 提取子字段
    request_headers = extract_body_field(body, "requestHeader")
    response_headers = extract_body_field(body, "responseHeader")
    response_data = extract_body_field(body, "responseData")
    query_string = extract_body_field(body, "queryString")
    method = extract_body_field(body, "method")
    cookies = extract_body_field(body, "cookies")

    # 提取断言
    assertions = extract_assertions(body)

    lines = []
    lines.append(f"--- 样本 #{index} ---")
    lines.append(f"  标签:     {label}")
    lines.append(f"  成功:     {success}")
    lines.append(f"  响应码:   {rc}")
    lines.append(f"  响应消息: {rm}")
    lines.append(f"  耗时:     {t}ms")
    if method:
        lines.append(f"  方法:     {method}")
    if query_string:
        lines.append(f"  请求体:   {query_string[:500]}")
    if request_headers:
        lines.append(f"  请求头:")
        for line in request_headers.split("\n"):
            if line.strip():
                lines.append(f"    {line.strip()}")
    if response_headers:
        lines.append(f"  响应头:")
        for line in response_headers.split("\n"):
            if line.strip():
                lines.append(f"    {line.strip()}")
    if response_data:
        lines.append(f"  响应体:   {response_data[:1000]}")
    if cookies:
        lines.append(f"  Cookies:  {cookies[:200]}")

    # 断言结果
    failed_assertions = [a for a in assertions if a["failure"] == "true"]
    if failed_assertions:
        lines.append(f"  断言失败 ({len(failed_assertions)} 个):")
        for a in failed_assertions:
            lines.append(f"    - 断言名: {a['name']}")
            if a["failureMessage"]:
                lines.append(f"      失败信息: {a['failureMessage'][:300]}")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(
        description="从 XML JTL 文件中提取失败请求的完整请求/响应数据"
    )
    parser.add_argument("jtl_file", help="JTL 文件路径")
    parser.add_argument(
        "--all", action="store_true", help="提取所有请求（默认只提取失败的）"
    )
    parser.add_argument("--label", help="按标签关键词过滤")
    parser.add_argument("--output", help="输出到文件（默认输出到控制台）")
    args = parser.parse_args()

    if not os.path.exists(args.jtl_file):
        print(f"ERROR: JTL 文件不存在: {args.jtl_file}")
        sys.exit(1)

    samples = parse_jtl(args.jtl_file)
    print(f"解析到 {len(samples)} 个样本", file=sys.stderr)

    # 过滤
    filtered = []
    for s in samples:
        attrs = s["attrs"]
        success = attrs.get("s", "true")
        label = attrs.get("lb", "")

        # 按标签过滤
        if args.label and args.label not in label:
            continue

        # 默认只提取失败的（排除预期失败）
        if not args.all:
            if success == "true":
                continue
            if "预期失败" in label:
                continue

        filtered.append(s)

    print(f"过滤后 {len(filtered)} 个样本", file=sys.stderr)

    # 格式化输出
    output_lines = []
    output_lines.append("=" * 80)
    output_lines.append(f"JTL 失败请求详情报告")
    output_lines.append(f"文件: {args.jtl_file}")
    output_lines.append(f"总样本数: {len(samples)}")
    output_lines.append(f"失败样本数: {len(filtered)}")
    output_lines.append("=" * 80)
    output_lines.append("")

    for i, sample in enumerate(filtered, 1):
        output_lines.append(format_sample(sample, i))
        output_lines.append("")

    output_text = "\n".join(output_lines)

    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(output_text)
        print(f"报告已写入: {args.output}", file=sys.stderr)
    else:
        print(output_text)


if __name__ == "__main__":
    main()
