package edu.byu.ece.rapidSmith.device.xdlrc;

import com.caucho.hessian.io.Hessian2Input;
import edu.byu.ece.rapidSmith.util.FileTools;

import java.io.IOException;
import java.nio.file.Path;

public interface XDLRCSource {
	void registerListener(XDLRCParserListener listener);
	void clearListeners();
	void parse() throws IOException;
	Path getFilePath();

	class XDLRCFileSource implements XDLRCSource {
		private XDLRCParser parser = new XDLRCParser();
		private Path xdlrcPath;

		public XDLRCFileSource(Path xdlrcPath) {
			this.xdlrcPath = xdlrcPath;
		}

		@Override
		public void registerListener(XDLRCParserListener listener) {
			parser.registerListener(listener);
		}

		@Override
		public void clearListeners() {
			parser.clearListeners();
		}

		@Override
		public void parse() throws IOException {
			parser.parse(xdlrcPath);
		}

		@Override
		public Path getFilePath() {
			return xdlrcPath;
		}
	}

	class CompressedXDLRCSource implements XDLRCSource {
		private CompressedXDLRCReader reader = new CompressedXDLRCReader();
		private Path cxdlrcPath;
		private CompressedXDLRC cxdlrc;

		public CompressedXDLRCSource(Path cxdlrcPath) {
			this.cxdlrcPath = cxdlrcPath;
		}

		public CompressedXDLRCSource(CompressedXDLRC cxdlrc) {
			this.cxdlrc = cxdlrc;
		}

		@Override
		public void registerListener(XDLRCParserListener listener) {
			reader.registerListener(listener);
		}

		@Override
		public void clearListeners() {
			reader.clearListeners();
		}

		@Override
		public void parse() throws IOException {
			if (cxdlrc == null) {
				Hessian2Input compactReader = FileTools.getCompactReader(cxdlrcPath);
				cxdlrc = (CompressedXDLRC) compactReader.readObject();
			}
			reader.traverse(cxdlrc);
		}

		@Override
		public Path getFilePath() {
			return cxdlrcPath;
		}
	}
}
