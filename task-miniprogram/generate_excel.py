import openpyxl
from openpyxl.styles import Font, Alignment, PatternFill, Border, Side
import datetime

def create_excel():
    # 创建工作簿
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = '学院任务管理系统功能清单'

    # 设置列宽
    column_widths = {'A': 20, 'B': 30, 'C': 40, 'D': 25, 'E': 35, 'F': 30}
    for col, width in column_widths.items():
        ws.column_dimensions[col].width = width

    # 创建样式
    title_font = Font(name='微软雅黑', size=14, bold=True, color='FFFFFF')
    title_fill = PatternFill(start_color='4472C4', end_color='4472C4', fill_type='solid')

    header_font = Font(name='微软雅黑', size=11, bold=True, color='FFFFFF')
    header_fill = PatternFill(start_color='5B9BD5', end_color='5B9BD5', fill_type='solid')

    data_font = Font(name='微软雅黑', size=10)

    category_font = Font(name='微软雅黑', size=11, bold=True, color='FFFFFF')
    category_fill = PatternFill(start_color='E74C3C', end_color='E74C3C', fill_type='solid')

    border_style = Border(
        left=Side(style='thin', color='D0D0D0'),
        right=Side(style='thin', color='D0D0D0'),
        top=Side(style='thin', color='D0D0D0'),
        bottom=Side(style='thin', color='D0D0D0')
    )

    # 设置标题行
    ws.merge_cells('A1:F1')
    title_cell = ws['A1']
    title_cell.value = '学院任务管理系统 - 功能清单'
    title_cell.font = title_font
    title_cell.fill = title_fill
    title_cell.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[1].height = 30

    # 设置表头
    headers = ['功能模块', '功能名称', '功能描述', '页面路径', '技术要点', '备注']
    for col, header in enumerate(headers, 1):
        cell = ws.cell(row=2, column=col)
        cell.value = header
        cell.font = header_font
        cell.fill = header_fill
        cell.alignment = Alignment(horizontal='center', vertical='center')
        cell.border = border_style

    ws.row_dimensions[2].height = 25

    # 功能数据
    functions_data = [
        ['用户认证与权限管理', '登录系统', '教职工号+密码登录，支持记住密码', '/pages/login/login', 'token存储、角色路由', ''],
        ['', '角色权限', '学院管理员/学院领导/科室领导/科室职员/普通用户', '', '权限控制、角色跳转', ''],
        ['', '记住密码', '本地存储账号密码功能', '', 'localStorage缓存', ''],

        ['首页任务管理', '任务列表展示', '卡片式布局，显示任务名称、状态、重要程度、负责人、截止时间', '/pages/index/index', '渐变色卡片设计', ''],
        ['', '关键词搜索', '支持任务名称、详情、创建人、负责人搜索', '', '前端实时搜索', ''],
        ['', '状态筛选', '按任务状态快速筛选（全部/进行中/待开始/已完成/已撤销）', '', '状态按钮筛选', ''],
        ['', '部门筛选', '左侧边栏部门导航，显示各部门任务数量', '', '侧边栏抽屉组件', ''],
        ['', '排序功能', '默认排序/创建时间/截止时间/优先级', '', '多维度排序', ''],
        ['', '高级筛选', '时间范围筛选+多条件组合筛选', '', '弹窗式筛选器', ''],
        ['', '下拉刷新', '手动刷新任务数据', '', 'scroll-view刷新', ''],
        ['', '自动刷新', '每10分钟自动刷新数据', '', '定时器机制', ''],
        ['', '缓存机制', '5分钟页面数据缓存', '', 'localStorage缓存', ''],

        ['任务申报功能', '基础信息', '任务名称/类型/学院/科室/重要程度/状态', '/pages/declaration/declaration', '多级选择器、表单验证', ''],
        ['', '时间管理', '开始时间/结束时间选择，结束时间不能早于开始时间', '', '日期选择器、逻辑验证', ''],
        ['', '负责人信息', '负责人设置（支持多人）、任务进度（0-100）', '', '多人逗号分隔、数字验证', ''],
        ['', '详细描述', '富文本编辑器（任务详情+结果）', '', 'editor组件', ''],
        ['', '附件管理', '文件上传、附件列表、删除功能、文件大小格式化', '', 'uni.uploadFile、格式化显示', ''],
        ['', '扩展字段', '任务属性、备注信息', '', '表单扩展字段', ''],
        ['', '表单验证', '必填项验证、逻辑验证、范围验证', '', '实时验证机制', ''],

        ['管理员后台', '用户信息管理', '用户列表、搜索、新增、修改、删除、停用/启用', '/pages/admin/admin', 'CRUD操作、状态管理', ''],
        ['', '科室信息管理', '科室列表、新增、修改、删除', '', '部门管理接口', ''],
        ['', '数据维护管理', '字典类型选择、树形展示、编辑/添加/删除', '', '字典数据管理、树形结构', ''],

        ['会议管理', '会议列表展示', '会议名称、状态、日期、时间、地点、标签', '/pages/meeting/meeting', '卡片式展示', ''],
        ['', '快速筛选', '全部/今日/明日/本周/本月快捷按钮', '', '快捷日期筛选', ''],
        ['', '自定义筛选', '日期范围筛选、排序方式选择', '', '弹窗式高级筛选', ''],
        ['', '会议详情', '查看会议详细信息', '/pages/meeting-detail/meeting-detail', '详情页面跳转', ''],
        ['', '搜索功能', '按会议名称、地点、主持人搜索', '', '关键词搜索', ''],

        ['重点工作管理', '功能特点', '部门筛选、状态筛选、搜索功能、排序功能', '/pages/works/works', '与任务管理类似功能', ''],
        ['', '工作详情', '查看工作详细信息', '/pages/work-detail/work-detail', '详情页面', ''],

        ['系统功能', '数据字典管理', '任务类型、重要程度、状态等字典配置', '', '字典接口、树形结构', ''],
        ['', '部门管理', '部门列表、统计、筛选', '', '部门接口、统计功能', ''],
        ['', '文件管理', '文件上传、附件管理、大小格式化', '', '文件上传接口', ''],
        ['', '性能优化', '分页加载、数据缓存、自动刷新', '', '分页接口、缓存策略', ''],

        ['界面设计', 'UI特色', '渐变设计、卡片布局、标签系统、响应式', '', '紫色渐变主题、现代化设计', ''],
        ['', '交互优化', '侧边栏、筛选器、日期选择器、搜索框', '', '抽屉组件、uView组件', ''],

        ['技术实现', '前端技术', 'uni-app、Vue.js、uView UI、富文本编辑器', '', '跨平台框架', ''],
        ['', '后端接口', '任务/会议/用户/部门/字典/文件管理接口', '', 'RESTful API', ''],
        ['', '数据缓存', 'localStorage、sessionStorage、5分钟缓存策略', '', '多级缓存机制', ''],

        ['导航与路由', '页面结构', '登录页/首页/申报/管理员/会议/工作/详情等', '', '页面路由配置', ''],
        ['', '底部导航', '主页/会议/重点工作/个人中心', '', '底部导航栏', ''],
    ]

    # 填充数据
    row_num = 3
    current_category = None
    category_row_start = None

    for row_data in functions_data:
        category, function_name, description, page_path, tech_points, notes = row_data

        if category and function_name:  # 有分类名，这是新分类的第一行
            if current_category and current_category != category:
                # 合并上一个分类的单元格
                ws.merge_cells(f'A{category_row_start}:A{row_num-1}')

            current_category = category
            category_row_start = row_num

            # 设置分类单元格
            category_cell = ws.cell(row=row_num, column=1)
            category_cell.value = category
            category_cell.font = category_font
            category_cell.fill = category_fill
            category_cell.alignment = Alignment(horizontal='center', vertical='center')
            category_cell.border = border_style

            # 设置其他列
            ws.cell(row=row_num, column=2).value = function_name
            ws.cell(row=row_num, column=3).value = description
            ws.cell(row=row_num, column=4).value = page_path
            ws.cell(row_num, column=5).value = tech_points
            ws.cell(row=row_num, column=6).value = notes

        elif not category and function_name:  # 同一分类的后续行
            # 设置分类单元格
            category_cell = ws.cell(row=row_num, column=1)
            category_cell.value = current_category
            category_cell.font = category_font
            category_cell.fill = category_fill
            category_cell.alignment = Alignment(horizontal='center', vertical='center')
            category_cell.border = border_style

            # 设置其他列
            ws.cell(row=row_num, column=2).value = function_name
            ws.cell(row=row_num, column=3).value = description
            ws.cell(row=row_num, column=4).value = page_path
            ws.cell(row=row_num, column=5).value = tech_points
            ws.cell(row=row_num, column=6).value = notes

        # 设置每行的样式
        for col in range(1, 7):
            cell = ws.cell(row=row_num, column=col)
            if col != 1 or not function_name:  # 非分类列或第一行
                cell.font = data_font
            cell.alignment = Alignment(horizontal='left' if col > 1 else 'center', vertical='center')
            cell.border = border_style

        ws.row_dimensions[row_num].height = 22
        row_num += 1

    # 合并最后一个分类
    if current_category and category_row_start:
        ws.merge_cells(f'A{category_row_start}:A{row_num-1}')

    # 添加统计信息
    row_num += 2
    ws.merge_cells(f'A{row_num}:F{row_num}')
    summary_cell = ws[f'A{row_num}']
    summary_cell.value = f'总计功能模块：10大类 | 功能数量：{len(functions_data)}项'
    summary_cell.font = Font(name='微软雅黑', size=12, bold=True, color='4472C4')
    summary_cell.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[row_num].height = 25

    # 添加生成时间
    row_num += 2
    ws.merge_cells(f'A{row_num}:F{row_num}')
    time_cell = ws[f'A{row_num}']
    time_cell.value = f'生成时间：{datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")}'
    time_cell.font = Font(name='微软雅黑', size=10, color='666666')
    time_cell.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[row_num].height = 20

    # 添加项目信息
    row_num += 1
    ws.merge_cells(f'A{row_num}:F{row_num}')
    project_cell = ws[f'A{row_num}']
    project_cell.value = '项目名称：学院任务管理系统（微信小程序） | 开发框架：uni-app + Vue.js + uView UI'
    project_cell.font = Font(name='微软雅黑', size=10, color='666666')
    project_cell.alignment = Alignment(horizontal='center', vertical='center')
    ws.row_dimensions[row_num].height = 20

    # 保存文件
    file_path = 'D:/wechat/wechat-miniprogram/学院任务管理系统功能清单.xlsx'
    wb.save(file_path)

    return file_path, len(functions_data)

if __name__ == '__main__':
    try:
        file_path, count = create_excel()
        print('[OK] Excel文件生成成功！')
        print(f'[FILE] 文件保存路径：{file_path}')
        print(f'[COUNT] 功能总项数：{count}')
        print('[MODULES] 包含功能模块：用户认证、首页任务管理、任务申报、管理员后台、会议管理、重点工作、系统功能、界面设计、技术实现、导航路由')
    except Exception as e:
        print(f'[ERROR] 生成失败：{e}')