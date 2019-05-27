package org.springframework.expression.spel.standard;

/**
 * 保存一种token, 其关联的数据, 及其在输入数据流中的位置 (start/end).
 */
class Token {

	TokenKind kind;

	String data;

	int startPos;  // 第一个字符的索引

	int endPos;  // 最后一个字符之后的char索引


	/**
	 * 在没有特定数据时使用的构造函数 (e.g. TRUE 或 '+')
	 * 
	 * @param startPos 确切的开始
	 * @param endPos 最后一个字符的索引
	 */
	Token(TokenKind tokenKind, int startPos, int endPos) {
		this.kind = tokenKind;
		this.startPos = startPos;
		this.endPos = endPos;
	}

	Token(TokenKind tokenKind, char[] tokenData, int startPos, int endPos) {
		this(tokenKind, startPos, endPos);
		this.data = new String(tokenData);
	}


	public TokenKind getKind() {
		return this.kind;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[").append(this.kind.toString());
		if (this.kind.hasPayload()) {
			s.append(":").append(this.data);
		}
		s.append("]");
		s.append("(").append(this.startPos).append(",").append(this.endPos).append(")");
		return s.toString();
	}

	public boolean isIdentifier() {
		return (this.kind == TokenKind.IDENTIFIER);
	}

	public boolean isNumericRelationalOperator() {
		return (this.kind == TokenKind.GT || this.kind == TokenKind.GE || this.kind == TokenKind.LT ||
				this.kind == TokenKind.LE || this.kind==TokenKind.EQ || this.kind==TokenKind.NE);
	}

	public String stringValue() {
		return this.data;
	}

	public Token asInstanceOfToken() {
		return new Token(TokenKind.INSTANCEOF, this.startPos, this.endPos);
	}

	public Token asMatchesToken() {
		return new Token(TokenKind.MATCHES, this.startPos, this.endPos);
	}

	public Token asBetweenToken() {
		return new Token(TokenKind.BETWEEN, this.startPos, this.endPos);
	}

}
