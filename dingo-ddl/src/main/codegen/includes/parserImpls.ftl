<#--
// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to you under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
-->

boolean IfNotExistsOpt() :
{
}
{
    <IF> <NOT> <EXISTS> { return true; }
|
    { return false; }
}

boolean IfExistsOpt() :
{
}
{
    <IF> <EXISTS> { return true; }
|
    { return false; }
}

SqlCreate SqlCreateSchema(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
}
{
    <SCHEMA> ifNotExists = IfNotExistsOpt() id = CompoundIdentifier()
    {
        return SqlDdlNodes.createSchema(s.end(this), replace, ifNotExists, id);
    }
}

SqlCreate SqlCreateForeignSchema(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
    SqlNode type = null;
    SqlNode library = null;
    SqlNodeList optionList = null;
}
{
    <FOREIGN> <SCHEMA> ifNotExists = IfNotExistsOpt() id = CompoundIdentifier()
    (
         <TYPE> type = StringLiteral()
    |
         <LIBRARY> library = StringLiteral()
    )
    [ optionList = Options() ]
    {
        return SqlDdlNodes.createForeignSchema(s.end(this), replace,
            ifNotExists, id, type, library, optionList);
    }
}

SqlNodeList Options() :
{
    final Span s;
    final List<SqlNode> list = new ArrayList<SqlNode>();
}
{
    <OPTIONS> { s = span(); } <LPAREN>
    [
        Option(list)
        (
            <COMMA>
            Option(list)
        )*
    ]
    <RPAREN> {
        return new SqlNodeList(list, s.end(this));
    }
}

void Option(List<SqlNode> list) :
{
    final SqlIdentifier id;
    final SqlNode value;
}
{
    id = SimpleIdentifier()
    value = Literal() {
        list.add(id);
        list.add(value);
    }
}

SqlNodeList TableElementList() :
{
    final Span s;
    final List<SqlNode> list = new ArrayList<SqlNode>();
}
{
    <LPAREN> { s = span(); }
    TableElement(list)
    (
        <COMMA> TableElement(list)
    )*
    <RPAREN> {
        return new SqlNodeList(list, s.end(this));
    }
}

void TableElement(List<SqlNode> list) :
{
    final SqlIdentifier id;
    final SqlDataTypeSpec type;
    final boolean nullable;
    final SqlNode e;
    final SqlNode constraint;
    SqlIdentifier name = null;
    final SqlNodeList columnList;
    final Span s = Span.of();
    final ColumnStrategy strategy;
}
{
    LOOKAHEAD(2) id = SimpleIdentifier()
    (
        type = DataType()
        nullable = NullableOptDefaultTrue()
        (
            [ <GENERATED> <ALWAYS> ] <AS> <LPAREN>
            e = Expression(ExprContext.ACCEPT_SUB_QUERY) <RPAREN>
            (
                <VIRTUAL> { strategy = ColumnStrategy.VIRTUAL; }
            |
                <STORED> { strategy = ColumnStrategy.STORED; }
            |
                { strategy = ColumnStrategy.VIRTUAL; }
            )
        |
            <DEFAULT_> e = Expression(ExprContext.ACCEPT_SUB_QUERY) {
                strategy = ColumnStrategy.DEFAULT;
            }
        |
            {
                e = null;
                strategy = nullable ? ColumnStrategy.NULLABLE
                    : ColumnStrategy.NOT_NULLABLE;
            }
        )
        {
            list.add(
                SqlDdlNodes.column(s.add(id).end(this), id,
                    type.withNullable(nullable), e, strategy));
        }
    |
        { list.add(id); }
    )
|
    id = SimpleIdentifier() {
        list.add(id);
    }
|
    [ <CONSTRAINT> { s.add(this); } name = SimpleIdentifier() ]
    (
        <CHECK> { s.add(this); } <LPAREN>
        e = Expression(ExprContext.ACCEPT_SUB_QUERY) <RPAREN> {
            list.add(SqlDdlNodes.check(s.end(this), name, e));
        }
    |
        <UNIQUE> { s.add(this); }
        columnList = ParenthesizedSimpleIdentifierList() {
            list.add(SqlDdlNodes.unique(s.end(columnList), name, columnList));
        }
    |
        <PRIMARY>  { s.add(this); } <KEY>
        columnList = ParenthesizedSimpleIdentifierList() {
            list.add(SqlDdlNodes.primary(s.end(columnList), name, columnList));
        }
    )
}

SqlNodeList AttributeDefList() :
{
    final Span s;
    final List<SqlNode> list = new ArrayList<SqlNode>();
}
{
    <LPAREN> { s = span(); }
    AttributeDef(list)
    (
        <COMMA> AttributeDef(list)
    )*
    <RPAREN> {
        return new SqlNodeList(list, s.end(this));
    }
}

void AttributeDef(List<SqlNode> list) :
{
    final SqlIdentifier id;
    final SqlDataTypeSpec type;
    final boolean nullable;
    SqlNode e = null;
    final Span s = Span.of();
}
{
    id = SimpleIdentifier()
    (
        type = DataType()
        nullable = NullableOptDefaultTrue()
    )
    [ <DEFAULT_> e = Expression(ExprContext.ACCEPT_SUB_QUERY) ]
    {
        list.add(SqlDdlNodes.attribute(s.add(id).end(this), id,
            type.withNullable(nullable), e, null));
    }
}

SqlCreate SqlCreateType(Span s, boolean replace) :
{
    final SqlIdentifier id;
    SqlNodeList attributeDefList = null;
    SqlDataTypeSpec type = null;
}
{
    <TYPE>
    id = CompoundIdentifier()
    <AS>
    (
        attributeDefList = AttributeDefList()
    |
        type = DataType()
    )
    {
        return SqlDdlNodes.createType(s.end(this), replace, id, attributeDefList, type);
    }
}

SqlCreate SqlCreateTable(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
    SqlNodeList tableElementList = null;
    SqlNode query = null;
    DingoTablePart dingoTablePart = null;
    String partNm = null;
    String comSymbol = null;
    String operand = null;
    Map<String,Object> attrList = null;
    List<DingoPartDetail> partList = null;
    String partType = null;
}
{
    <TABLE> ifNotExists = IfNotExistsOpt() id = CompoundIdentifier()
    [ tableElementList = TableElementList() ]
    [ <WITH> attrList = AttrMap() ]
    [
       <PARTITION> <BY>
       [
            <RANGE>
            { partType = "RANGE"; }
            dingoTablePart = partColNm()
            partList = defPartNew()
            { dingoTablePart = new DingoTablePart(dingoTablePart.getFuncNm(), dingoTablePart.getCols());
              dingoTablePart.setPartDetailList(partList);
              return DingoSqlDdlNodes.createTable(s.end(this), replace, ifNotExists, id,
                          tableElementList, query, attrList, partType, dingoTablePart);
             }
       ]
    ]
    [ <AS> query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY) ]
    {
        return DingoSqlDdlNodes.createTable(s.end(this), replace, ifNotExists, id,
            tableElementList, query, attrList, partType, dingoTablePart);
    }
}

List<DingoPartDetail> defPart() : {
    List<DingoPartDetail> listParts = new ArrayList<DingoPartDetail>();
    Object partNm = null;
    String operator = null;
    List<Object> operand = null;
}{
    <LPAREN>
    [
       <PARTITION>
       partNm = anything()
       <VALUES>
       operator = symbol()
       <THAN>
        [ operand = getPartVals() ]
        [ <MAXVALUE> { operand = new ArrayList<Object>(); operand.add("MAXVALUE"); } ]
       { listParts.add(new DingoPartDetail(partNm, operator, operand)); }
       { partNm = null; operator = null; operand = null; }
       (
          <COMMA>
          <PARTITION>
          partNm = anything()
          <VALUES>
          operator = symbol()
          <THAN>
          [ operand = getPartVals() ]
          [ <MAXVALUE> { operand = new ArrayList<Object>(); operand.add("MAXVALUE"); } ]
          { listParts.add(new DingoPartDetail(partNm, operator, operand )); }
          { partNm = null; operator = null; operand = null; }
       )*
    ]
    <RPAREN>
    { return listParts; }
}

List<DingoPartDetail> defPartNew() : {
    List<DingoPartDetail> listParts = new ArrayList<DingoPartDetail>();
    Object partNm = null;
    String operator = null;
    List<Object> operand = null;
}{
    <LPAREN>
    [
       <VALUES>
       operand = getPartVals()
       { listParts.add(new DingoPartDetail(partNm, operator, operand)); }
       { partNm = null; operator = null; operand = null; }
       (
          <COMMA>
          operand = getPartVals()
          { listParts.add(new DingoPartDetail(partNm, operator, operand )); }
          { partNm = null; operator = null; operand = null; }
       )*
    ]
    <RPAREN>
    { return listParts; }
}

Object getPartPairVal() : {
   Object operand = null;
}{
     <LPAREN>
     [
        operand = anything()
     ]
     <RPAREN>
     { return operand; }
}

List<Object> getPartVals() : {
   List<Object> partVals = new ArrayList<Object>();
   Object operand = null;
}{
     <LPAREN>
     [
        operand = anything()
        { partVals.add(operand); operand = null; }
        (
          <COMMA>
          operand = anything()
          { partVals.add(operand); operand = null; }
        )*
     ]
     <RPAREN>
     { return partVals; }
}

DingoTablePart partColNm() : {
	String partColNm = null;
	List<String> partColNmList = new ArrayList<String>();
	String funNm = null;
	DingoTablePart tablePart = null;
	SqlIdentifier tmp = null;
} {
	  <LPAREN>
	  [
	    tmp = CompoundIdentifier()
	    { partColNm = tmp.names.get(0); partColNmList.add(partColNm); }
	    [
	      <LPAREN>
	      { partColNmList.remove(partColNm); funNm = partColNm; partColNm = null; tmp = null;}
	      tmp = CompoundIdentifier()
          { partColNm = tmp.names.get(0); partColNmList.add(partColNm); partColNm = null; tmp = null; }
	      (
	         <COMMA>
	         tmp = CompoundIdentifier()
             { partColNm = tmp.names.get(0); partColNmList.add(partColNm); partColNm = null; tmp = null; }
	      )*
	      <RPAREN>
	    ]
	    (
	      <COMMA>
          tmp = CompoundIdentifier()
          { partColNm = tmp.names.get(0); partColNmList.add(partColNm); partColNm = null; tmp = null; }
	    )*
	  ]
	  <RPAREN>
	{ tablePart = new DingoTablePart();
	  tablePart.setFuncNm(funNm); tablePart.setCols(partColNmList); return tablePart; }
}



Map<String,Object> AttrMap() : {
	final Map<String,Object> map = new HashMap<String,Object>();
	String key = null;
	Object tmp = null;
	Object value = null;
}{
	<LPAREN>
	[
		tmp = anything()
		{ key=tmp.toString(); }
		<EQ>
		value = anything()
		{ map.put(key, value); }
		{ key = null; tmp = null; value = null; }
		(
			<COMMA>
			tmp = anything()
			{ key=tmp.toString(); }
			<EQ>
			value = anything()
			{ map.put(key, value); }
			{ key = null; tmp = null; value = null; }
		)*
	]
	<RPAREN>
	{ return map; }
}

String symbol() : {
}{
	<IDENTIFIER>
	{ return token.image.toUpperCase(); }
}

String getPartCol() : {
}{
	<IDENTIFIER>
	{
        return unquotedIdentifier();

    }
}

Object nullValue(): {}{
	<NULL>
	{ return null; }
}

Object anything() : {
	Object x;
}{
	(
	  x = symbol()
	| x = number()
	| x = booleanValue()
	| x = NonReservedKeyWord()
	| x = nullValue()
	)
	{ return x; }
}

Boolean booleanValue(): {
	Boolean b;
}{
	(
		(
			<TRUE>
			{ b = Boolean.TRUE; }
		) | (
			<FALSE>
			{ b = Boolean.FALSE; }
		)
	)
	{ return b; }
}

Number number(): {
	Token t;
}{
	 (
        t = <UNSIGNED_INTEGER_LITERAL>
        {
            if(nativeNumbers) {
                return new Double(t.image);
            } else {
                return new BigInteger(substringBefore(t.image, '.'));
            }
        }
      )
}

SqlCreate SqlCreateView(Span s, boolean replace) :
{
    final SqlIdentifier id;
    SqlNodeList columnList = null;
    final SqlNode query;
}
{
    <VIEW> id = CompoundIdentifier()
    [ columnList = ParenthesizedSimpleIdentifierList() ]
    <AS> query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY) {
        return SqlDdlNodes.createView(s.end(this), replace, id, columnList,
            query);
    }
}

SqlCreate SqlCreateMaterializedView(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
    SqlNodeList columnList = null;
    final SqlNode query;
}
{
    <MATERIALIZED> <VIEW> ifNotExists = IfNotExistsOpt()
    id = CompoundIdentifier()
    [ columnList = ParenthesizedSimpleIdentifierList() ]
    <AS> query = OrderedQueryOrExpr(ExprContext.ACCEPT_QUERY) {
        return SqlDdlNodes.createMaterializedView(s.end(this), replace,
            ifNotExists, id, columnList, query);
    }
}

private void FunctionJarDef(SqlNodeList usingList) :
{
    final SqlDdlNodes.FileType fileType;
    final SqlNode uri;
}
{
    (
        <ARCHIVE> { fileType = SqlDdlNodes.FileType.ARCHIVE; }
    |
        <FILE> { fileType = SqlDdlNodes.FileType.FILE; }
    |
        <JAR> { fileType = SqlDdlNodes.FileType.JAR; }
    ) {
        usingList.add(SqlLiteral.createSymbol(fileType, getPos()));
    }
    uri = StringLiteral() {
        usingList.add(uri);
    }
}

SqlCreate SqlCreateFunction(Span s, boolean replace) :
{
    final boolean ifNotExists;
    final SqlIdentifier id;
    final SqlNode className;
    SqlNodeList usingList = SqlNodeList.EMPTY;
}
{
    <FUNCTION> ifNotExists = IfNotExistsOpt()
    id = CompoundIdentifier()
    <AS>
    className = StringLiteral()
    [
        <USING> {
            usingList = new SqlNodeList(getPos());
        }
        FunctionJarDef(usingList)
        (
            <COMMA>
            FunctionJarDef(usingList)
        )*
    ] {
        return SqlDdlNodes.createFunction(s.end(this), replace, ifNotExists,
            id, className, usingList);
    }
}

SqlDrop SqlDropSchema(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
    final boolean foreign;
}
{
    (
        <FOREIGN> { foreign = true; }
    |
        { foreign = false; }
    )
    <SCHEMA> ifExists = IfExistsOpt() id = CompoundIdentifier() {
        return SqlDdlNodes.dropSchema(s.end(this), foreign, ifExists, id);
    }
}

SqlDrop SqlDropType(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
}
{
    <TYPE> ifExists = IfExistsOpt() id = CompoundIdentifier() {
        return SqlDdlNodes.dropType(s.end(this), ifExists, id);
    }
}

SqlDrop SqlDropTable(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
}
{
    <TABLE> ifExists = IfExistsOpt() id = CompoundIdentifier() {
        return SqlDdlNodes.dropTable(s.end(this), ifExists, id);
    }
}

SqlDrop SqlDropView(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
}
{
    <VIEW> ifExists = IfExistsOpt() id = CompoundIdentifier() {
        return SqlDdlNodes.dropView(s.end(this), ifExists, id);
    }
}

SqlDrop SqlDropMaterializedView(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
}
{
    <MATERIALIZED> <VIEW> ifExists = IfExistsOpt() id = CompoundIdentifier() {
        return SqlDdlNodes.dropMaterializedView(s.end(this), ifExists, id);
    }
}

SqlDrop SqlDropFunction(Span s, boolean replace) :
{
    final boolean ifExists;
    final SqlIdentifier id;
}
{
    <FUNCTION> ifExists = IfExistsOpt()
    id = CompoundIdentifier() {
        return SqlDdlNodes.dropFunction(s.end(this), ifExists, id);
    }
}
