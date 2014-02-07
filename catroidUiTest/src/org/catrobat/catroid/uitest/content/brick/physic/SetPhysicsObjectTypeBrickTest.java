package org.catrobat.catroid.uitest.content.brick.physic;

import android.widget.ListView;
import android.widget.Spinner;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.physic.PhysicsObject.Type;
import org.catrobat.catroid.physic.content.bricks.SetPhysicsObjectTypeBrick;
import org.catrobat.catroid.ui.ScriptActivity;
import org.catrobat.catroid.ui.adapter.BrickAdapter;
import org.catrobat.catroid.uitest.util.BaseActivityInstrumentationTestCase;
import org.catrobat.catroid.uitest.util.UiTestUtils;

import java.util.ArrayList;

public class SetPhysicsObjectTypeBrickTest extends BaseActivityInstrumentationTestCase<ScriptActivity> {

	private Project project;
	private SetPhysicsObjectTypeBrick setPhysicsObjectTypeBrick;

	public SetPhysicsObjectTypeBrickTest() {
		super(ScriptActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		// normally super.setUp should be called first
		// but kept the test failing due to view is null
		// when starting in ScriptActivity
		createProject();
		super.setUp();
	}

	public void testPhysicsObjectTypeBrick() {
		ListView dragDropListView = UiTestUtils.getScriptListView(solo);
		BrickAdapter adapter = (BrickAdapter) dragDropListView.getAdapter();

		int childrenCount = adapter.getChildCountFromLastGroup();
		int groupCount = adapter.getScriptCount();

		assertEquals("Incorrect number of bricks.", 2, dragDropListView.getChildCount());
		assertEquals("Incorrect number of bricks.", 1, childrenCount);

		ArrayList<Brick> projectBrickList = project.getSpriteList().get(0).getScript(0).getBrickList();
		assertEquals("Incorrect number of bricks.", 1, projectBrickList.size());

		assertEquals("Wrong Brick instance.", projectBrickList.get(0), adapter.getChild(groupCount - 1, 0));
		assertNotNull("TextView does not exist.", solo.getText(solo.getString(R.string.brick_set_physics_object_type)));

		String[] typeArray = solo.getCurrentActivity().getResources().getStringArray(R.array.physics_object_types);
		for (int i = 0; i < typeArray.length; i++) {
			solo.clickOnView(solo.getCurrentActivity().findViewById(R.id.brick_set_physics_object_type_spinner));
			solo.clickInList(i + 1);
			assertTrue("Correct item in Spinner not shown", solo.waitForText(typeArray[i]));
			assertEquals("Wrong physics type", typeArray[i],
					((Spinner) solo.getView(R.id.brick_set_physics_object_type_spinner)).getSelectedItem().toString());
		}
	}

	private void createProject() {
		project = new Project(null, UiTestUtils.DEFAULT_TEST_PROJECT_NAME);
		Sprite sprite = new Sprite("cat");
		Script script = new StartScript(sprite);
		setPhysicsObjectTypeBrick = new SetPhysicsObjectTypeBrick(sprite, Type.DYNAMIC);
		script.addBrick(setPhysicsObjectTypeBrick);

		sprite.addScript(script);
		project.addSprite(sprite);

		ProjectManager.getInstance().setProject(project);
		ProjectManager.getInstance().setCurrentSprite(sprite);
		ProjectManager.getInstance().setCurrentScript(script);

	}

}
